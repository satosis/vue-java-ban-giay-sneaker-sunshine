package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.dto.request.AddressCustomerRequest;
import com.example.duantotnghiep.dto.request.CustomerBlacklistRequest;
import com.example.duantotnghiep.dto.request.CustomerRequest;
import com.example.duantotnghiep.dto.request.QuenMatKhauReq;
import com.example.duantotnghiep.dto.response.AddressCustomerResponse;
import com.example.duantotnghiep.dto.response.BadCustomerResponse;
import com.example.duantotnghiep.dto.response.CustomerResponse;
import com.example.duantotnghiep.mapper.AddressMapper;
import com.example.duantotnghiep.mapper.CustomerMapper;
import com.example.duantotnghiep.mapper.UserMapper;
import com.example.duantotnghiep.model.AddressCustomer;
import com.example.duantotnghiep.model.Customer;
import com.example.duantotnghiep.model.CustomerBlacklistHistory;
import com.example.duantotnghiep.model.User;
import com.example.duantotnghiep.repository.AddressRepository;
import com.example.duantotnghiep.repository.CustomerBlacklistHistoryRepository;
import com.example.duantotnghiep.repository.CustomerRepository;
import com.example.duantotnghiep.repository.UserRepository;
import com.example.duantotnghiep.service.AccountEmailService;
import com.example.duantotnghiep.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CustomerMapper customerMapper;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final CustomerBlacklistHistoryRepository customerBlacklistHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountEmailService accountEmailService;


    @Transactional(rollbackFor = Throwable.class)
    @Override
    public CustomerResponse createCustomer(CustomerRequest request) throws Exception {
        // 1. Chuyển CustomerRequest thành Customer entity
        Customer customer = userMapper.toCustomerEntity(request);
        String username = request.getUsername();
        boolean exists = userRepository.existsActiveByUsername(username);
        boolean existsPhone = userRepository.existsActiveByPhone(request.getPhone());

        if (exists) {
            // ĐÃ tồn tại user đang active với username này
            throw new IllegalArgumentException("Username đã tồn tại (active)");
        }

        if (existsPhone) {
            // ĐÃ tồn tại user đang active với username này
            throw new IllegalArgumentException("Phone đã tồn tại ");
        }

        customer.setCustomerCode(generateCustomerCode());
        customer.setCreatedDate(LocalDateTime.now());
        customer.setCreatedBy("admin");
        customer.setStatus(1);

        // 2. Lưu customer trước để có id cho các quan hệ liên quan
        customerRepository.save(customer);

        // 3. Tạo AddressCustomer
        AddressCustomer address = addressMapper.toAddressCustomerEntitty(request);
        address.setCreatedDate(new Date());
        address.setCreatedBy("admin");
        address.setStatus(1);
        address.setDefaultAddress(true);
        address.setCustomer(customer); // gán quan hệ
        addressRepository.save(address); // lưu address sau khi có customer

        // 4. Set địa chỉ mặc định

        customerRepository.save(customer); // cập nhật lại customer để set default address

        // 5. Tạo user login
        User user = userMapper.toCustomerUserEntity(request);
        user.setCreatedAt(new Date());
        user.setCreatedBy("admin");
        user.setRole(3);
        user.setCustomer(customer);
        userRepository.save(user);

        // 6. Trả về response
        return customerMapper.toDto(customer);
    }

    @Transactional
    @Override
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        // Tìm user theo customerId
        User user = userRepository.findByCustomerId(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));

        // Lấy ra customer từ user
        Customer customer = user.getCustomer();
        if (customer == null) {
            throw new RuntimeException("User không chứa thông tin customer");
        }

        // Cập nhật thông tin customer từ request
        userMapper.updateCustomerFromRequest(request, customer);
        customer.setUpdatedBy("admin"); // hoặc người dùng đang đăng nhập
        customer.setUpdatedDate(LocalDateTime.now());

        // Cập nhật thông tin user
        // Cập nhật thông tin user (chỉ khi FE gửi lên)
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername().trim());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(request.getPassword());
        }
        user.setUpdatedAt(new Date());
        user.setUpdatedBy("admin");


        // Gán lại customer vào user nếu cần
        user.setCustomer(customer);

        // Lưu lại cả hai
        customerRepository.save(customer);
        userRepository.save(user);

        // Trả về DTO phản hồi
        return userMapper.toCustomerResponse(user);
    }


    @Transactional
    @Override
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findCustomerByIdAndStatus(id,1)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + id));

        customer.setStatus(0);
        customer.setUpdatedDate(LocalDateTime.now());
        customer.setUpdatedBy("admin"); // hoặc username hiện tại nếu có
        customerRepository.save(customer);

        List<AddressCustomer> addressList = addressRepository.findByCustomerIdAndStatusOrderByDefaultAddressDesc(id,1);
        for (AddressCustomer address : addressList) {
            address.setStatus(0);
            address.setUpdatedDate(new Date());
            address.setUpdatedBy("admin");
        }
        addressRepository.saveAll(addressList);
    }

    @Override
    public CustomerResponse getCustomerById(Long id) {
        // Lấy thông tin customer
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // Tìm địa chỉ mặc định của khách hàng (nếu có)
        AddressCustomer address = addressRepository.findFirstByCustomerIdAndDefaultAddressTrue(customer.getId());

        // Ánh xạ customer sang response DTO
        CustomerResponse response = customerMapper.toDto(customer);

        // Nếu có địa chỉ thì set vào DTO
        if (address != null) {
            response.setCountry(address.getCountry());
            response.setProvinceCode(address.getProvinceCode());
            response.setProvinceName(address.getProvinceName());
            response.setDistrictCode(address.getDistrictCode());
            response.setDistrictName(address.getDistrictName());
            response.setWardCode(address.getWardCode());
            response.setWardName(address.getWardName());
            response.setHouseName(address.getHouseName());
        }

        return response;
    }

    @Override
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(customerMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<CustomerResponse> findCustomerByStatus(Pageable pageable) {
        Page<Customer> customers = customerRepository.findCustomerByStatus1(1, pageable);
        return customers.map(customerMapper::toDto);
    }

    @Override
    public List<AddressCustomerResponse> getByCustomerId(Long customerId) {
        return addressRepository.findByCustomerIdAndStatusOrderByDefaultAddressDesc(customerId,1)
                .stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AddressCustomerResponse create(AddressCustomerRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        AddressCustomer address = addressMapper.toEntity(request);
        address.setCustomer(customer);
        address.setDefaultAddress(false);
        address.setCreatedDate(new Date());
        address.setCreatedBy("admin"); // có thể là user đang đăng nhập
        address.setStatus(1);

        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Override
    public AddressCustomerResponse getAddressById(Long id) {
        AddressCustomer address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        return addressMapper.toResponse(address);
    }

    @Override
    public AddressCustomerResponse update(Long id, AddressCustomerRequest request) {
        AddressCustomer address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        addressMapper.updateEntityFromRequest(request, address);
        address.setUpdatedDate(new Date());
        address.setUpdatedBy("admin");

        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Override
    public void deleteAddressCustomer(Long id) {
        AddressCustomer address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        address.setStatus(0);
        address.setUpdatedDate(new Date());
        address.setUpdatedBy("admin");
        addressRepository.save(address);

    }

    @Transactional
    @Override
    public void setDefaultAddress(Long customerId, Long addressId) {
        // Bước 1: Set tất cả địa chỉ của customerId về false
        addressRepository.clearDefaultAddress(customerId);

        // Bước 2: Set địa chỉ được chọn thành true
        AddressCustomer address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        if (!address.getCustomer().getId().equals(customerId)) {
            throw new RuntimeException("Địa chỉ không thuộc khách hàng này");
        }

        address.setDefaultAddress(true);
        addressRepository.save(address); // Chỉ cần save địa chỉ được chọn
    }


    private String generateCustomerCode() {
        String prefix = "CUSTOMER-";
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + datePart + "-" + randomPart;
    }

    @Override
    public void blacklistCustomer(Long id, CustomerBlacklistRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));

        // ✅ Kiểm tra nếu đã bị cấm
        if (Boolean.TRUE.equals(customer.getIsBlacklisted())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khách hàng đã bị cấm trước đó");
        }

        // ✅ Cập nhật trạng thái bị cấm
        customer.setIsBlacklisted(true);
        customer.setBlacklistReason(request.getReason());
        customer.setBlacklistExpiryDate(LocalDateTime.now().plusDays(request.getDurationInDays()));
        customer.setTrustScore(Math.max(0, customer.getTrustScore() == null ? 0 : customer.getTrustScore() - 20));

        customerRepository.save(customer);

        // ✅ Ghi lại lịch sử cấm
        CustomerBlacklistHistory history = new CustomerBlacklistHistory();
        history.setCustomer(customer);
        history.setReason(request.getReason());
        history.setStartTime(LocalDateTime.now());
        history.setEndTime(LocalDateTime.now().plusDays(request.getDurationInDays()));

        customerBlacklistHistoryRepository.save(history);
    }

    @Override
    public void unblacklistCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));

        // Cập nhật lại thông tin khách hàng
        customer.setIsBlacklisted(false);
        customer.setTrustScore(100); // Reset điểm về 100
        customer.setBlacklistReason(null);
        customer.setBlacklistExpiryDate(null);
        customerRepository.save(customer);
        customer.setLastBlacklistCancelCount(0); // Reset để lần sau chỉ cấm nếu hủy > 5 đơn mới

        // Lấy tên người thực hiện từ SecurityContext
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        // Ghi lịch sử gỡ cấm
        CustomerBlacklistHistory history = new CustomerBlacklistHistory();
        history.setCustomer(customer);
        history.setReason("Gỡ cấm khách hàng, điểm uy tín được đặt lại về 100");
        history.setStartTime(LocalDateTime.now()); // thời điểm thực hiện hành động
        history.setUnblacklistedBy(currentUsername); // người thực hiện
        history.setNotes(null); // nếu cần ghi chú thêm

        customerBlacklistHistoryRepository.save(history);
    }

    @Override
    public List<BadCustomerResponse> getAllBlacklistedCustomers() {
        List<Customer> blacklistedCustomers = customerRepository.findByIsBlacklistedTrue();
        return customerMapper.toBadCustomerDtoList(blacklistedCustomers);
    }

    @Override
    public void QuenMatKhauClient(QuenMatKhauReq req) {
        String email = req.getEmail() == null ? null  : req.getEmail().trim();
        String phone = req.getPhone() == null ? null  : req.getPhone().trim();
        Optional<Customer> c = customerRepository.findCustomerByPhoneAndEmail(phone, email);
        if(!c.isPresent()) {
            throw new IllegalArgumentException("Số điện thoại hoặc email chưa đúng");
        }
        Long idCustomer =  c.get().getId();
        User u = userRepository.findByCustomerId(idCustomer).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom rnd = new SecureRandom();
        StringBuilder pw = new StringBuilder();
        for (int i = 0; i < 6; i++) pw.append(chars.charAt(rnd.nextInt(chars.length())));
        String rawPassword = passwordEncoder.encode(pw.toString());

        u.setPassword(rawPassword);
        userRepository.save(u);
        String customerName = c.get().getCustomerName();
        String username     = u.getUsername();
        accountEmailService.sendPasswordResetEmail(email, customerName, username, rawPassword);

    }

    @Override
    public Page<CustomerResponse> searchByNameAndPhone(String name, String phone, String phoneSuffix, Pageable pageable) {
        Page<Customer> customers = customerRepository.searchByNameAndPhone(name, phone, phoneSuffix, pageable);
        return customers.map(customerMapper::toDto);
    }



// xuất excel lấy ra thông tin địa chỉ khách hàng

    @Override
    public List<CustomerResponse> findAllForExport(String name, String phone) {
        // lấy toàn bộ KH (hoặc theo filter) ở dạng entity -> dto
        var dtoPage = this.searchByNameAndPhone(name, phone, null, Pageable.unpaged());
        var dtos = dtoPage.getContent();
        if (dtos.isEmpty()) return dtos;

        var ids = dtos.stream().map(CustomerResponse::getId).toList();
        var addresses = addressRepository.findBestAddressesForCustomerIds(ids);


        // map nhanh theo customerId
        var map = new java.util.HashMap<Long, AddressCustomer>();
        for (var a : addresses) {
            map.put(a.getCustomer().getId(), a);
        }
        for (var dto : dtos) {
            var a = map.get(dto.getId());
            if (a != null) {
                dto.setCountry(a.getCountry());
                dto.setProvinceCode(a.getProvinceCode());
                dto.setProvinceName(a.getProvinceName());
                dto.setDistrictCode(a.getDistrictCode());
                dto.setDistrictName(a.getDistrictName());
                dto.setWardCode(a.getWardCode());
                dto.setWardName(a.getWardName());
                dto.setHouseName(a.getHouseName());
            }
        }
        return dtos;
    }





}
