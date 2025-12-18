package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.Exception.DiscountCampaignInvalidException;
import com.example.duantotnghiep.Exception.VoucherInvalidException;
import com.example.duantotnghiep.dto.PaymentSummary;
import com.example.duantotnghiep.dto.request.AddressRequest;
import com.example.duantotnghiep.dto.request.CartItemRequest;
import com.example.duantotnghiep.dto.request.InvoiceRequest;
import com.example.duantotnghiep.dto.request.InvoiceSearchRequest;
import com.example.duantotnghiep.dto.response.*;
import com.example.duantotnghiep.mapper.InvoiceMapper;
import com.example.duantotnghiep.model.*;
import com.example.duantotnghiep.repository.*;
import com.example.duantotnghiep.service.AccountEmailService;
import com.example.duantotnghiep.service.InvoiceService;
import com.example.duantotnghiep.service.VoucherEmailService;
import com.example.duantotnghiep.service.VoucherService;
import com.example.duantotnghiep.state.CustomerTier;
import com.example.duantotnghiep.state.TrangThaiChiTiet;
import com.example.duantotnghiep.state.TrangThaiTong;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceDetailRepository invoiceDetailRepository;
    private final ProductDetailRepository productDetailRepository;
    private final ProductRepository productRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherHistoryRepository voucherHistoryRepository;
    private final InvoiceMapper invoiceMapper;
    private static final String DEFAULT_CUSTOMER_NAME = "Khách lẻ";
    private final UserRepository userRepository;
    private final VoucherEmailService voucherEmailService;
    private final EmployeeRepository employeeRepository;
    private final InvoiceTransactionRepository invoiceTransactionRepository;
    private final ZaloPayService zaloPayService;
    private final VnpayService vnpayService;
    private final DiscountCampaignRepository discountCampaignRepository;
    private final CustomerBlacklistHistoryRepository customerBlacklistHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountEmailService accountEmailService;
    private final ProductCategoryRepository productCategoryRepository;
    private final ReservationOrderRepository reservationOrderRepository;
    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final int MAX_OPEN_INVOICES = 5;
    private final InvoiceEmailService invoiceEmailService;

    @Transactional
    @Override
    public InvoiceResponse createEmptyInvoice() {
        String username = currentUsername();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với username: " + username));

        Employee employee = user.getEmployee();
        if (employee == null) throw new RuntimeException("Người dùng không phải là nhân viên.");

        //  Giới hạn: tối đa 5 hóa đơn đang xử lý cho nhân viên này ở quầy (orderType=0)
        long openCount = invoiceRepository.countByStatusAndOrderTypeAndEmployee_Id(
                TrangThaiTong.DANG_XU_LY, 0, employee.getId()
        );
        if (openCount >= MAX_OPEN_INVOICES) {
            // ném lỗi 400 dễ hiểu
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Bạn chỉ có thể tạo tối đa " + MAX_OPEN_INVOICES + " hóa đơn đang xử lý."
            );
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceCode(safeInvoiceCode());
        invoice.setEmployee(employee);
        invoice.setCreatedDate(new Date());
        invoice.setUpdatedDate(new Date());
        invoice.setStatus(TrangThaiTong.DANG_XU_LY);
        invoice.setStatusDetail(TrangThaiChiTiet.CHO_XU_LY);
        invoice.setTotalAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RM));
        invoice.setDiscountAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RM));
        invoice.setFinalAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RM));
        invoice.setShippingFee(BigDecimal.ZERO.setScale(MONEY_SCALE, RM));
        invoice.setCreatedBy(username);
        invoice.setDescription("Hóa đơn bán tại quầy");
        invoice.setOrderType(0);
        invoice.setIsPaid(false);

        invoiceRepository.save(invoice);
        return invoiceMapper.toInvoiceResponse(invoice);
    }


    @Transactional
    public void applyDiscountToInvoiceDetails(Invoice invoice) {
        if (invoice == null) return;

        final LocalDateTime now = LocalDateTime.now();
        final String username = currentUsername();

        final List<DiscountCampaign> activeCandidates = Optional
                .ofNullable(discountCampaignRepository.findActiveCampaigns(now))
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(c -> c != null
                        && Objects.equals(c.getStatus(), 1)
                        && (c.getStartDate() == null || !now.isBefore(c.getStartDate()))
                        && (c.getEndDate()   == null || !now.isAfter(c.getEndDate())))
                .toList();

        final java.util.function.BiPredicate<DiscountCampaign, Long> appliesToPd = (c, pdId) ->
                pdId != null
                        && c.getProductDetails() != null
                        && c.getProductDetails().stream().anyMatch(l ->
                        l != null && l.getProductDetail() != null
                                && Objects.equals(l.getProductDetail().getId(), pdId));

        final java.util.function.BiPredicate<DiscountCampaign, Long> appliesToProduct = (c, productId) ->
                productId != null
                        && c.getProducts() != null
                        && c.getProducts().stream().anyMatch(l ->
                        l != null && l.getProduct() != null
                                && Objects.equals(l.getProduct().getId(), productId));

        final List<InvoiceDetail> details = invoiceDetailRepository.findByInvoiceAndStatus(invoice, 1);

        boolean anyCampaignDetached = false;
        List<String> affectedProducts = new ArrayList<>();

        for (InvoiceDetail d : details) {
            if (d == null || d.getProductDetail() == null) continue;

            ProductDetail pd = d.getProductDetail();
            Long pdId = pd.getId();
            Long productId = (pd.getProduct() != null) ? pd.getProduct().getId() : null;

            // Giá gốc
            BigDecimal basePrice = money(d.getSellPrice());
            if (basePrice.compareTo(BigDecimal.ZERO) < 0) basePrice = BigDecimal.ZERO;

            int finalPercent = 0;
            DiscountCampaign chosen = null;

            // Ghi nhận campaign trước đó trên dòng
            DiscountCampaign oldCampaign = d.getDiscountCampaign();

            // 1) Nếu dòng đã gắn campaign → chỉ dùng khi campaign đang hoạt động & đúng đối tượng
            DiscountCampaign lineDc = oldCampaign;
            if (lineDc != null
                    && Objects.equals(lineDc.getStatus(), 1)
                    && (lineDc.getStartDate() == null || !now.isBefore(lineDc.getStartDate()))
                    && (lineDc.getEndDate() == null   || !now.isAfter(lineDc.getEndDate()))) {

                BigDecimal pctPd = Optional.ofNullable(lineDc.getProductDetails()).orElseGet(Collections::emptyList)
                        .stream()
                        .filter(l -> l.getProductDetail() != null && Objects.equals(l.getProductDetail().getId(), pdId))
                        .map(DiscountCampaignProductDetail::getDiscountPercentage)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);

                if (pctPd != null) {
                    finalPercent = clampPercent(pctPd.setScale(0, RM).intValue());
                    chosen = lineDc;
                } else if (appliesToProduct.test(lineDc, productId) && lineDc.getDiscountPercentage() != null) {
                    finalPercent = clampPercent(lineDc.getDiscountPercentage().setScale(0, RM).intValue());
                    chosen = lineDc;
                }
            }

            // 2) Nếu chưa có %, tự tìm best trong danh sách activeCandidates
            if (finalPercent == 0) {
                Optional<Map.Entry<DiscountCampaign, BigDecimal>> bestPd = activeCandidates.stream()
                        .filter(c -> appliesToPd.test(c, pdId))
                        .flatMap(c -> c.getProductDetails().stream()
                                .filter(l -> l.getProductDetail() != null
                                        && Objects.equals(l.getProductDetail().getId(), pdId))
                                .map(l -> Map.entry(
                                        c,
                                        l.getDiscountPercentage() != null
                                                ? l.getDiscountPercentage()
                                                : Optional.ofNullable(c.getDiscountPercentage()).orElse(BigDecimal.ZERO)
                                )))
                        .max(Comparator.comparing(e -> e.getValue(), Comparator.nullsFirst(BigDecimal::compareTo)));

                if (bestPd.isPresent()) {
                    chosen = bestPd.get().getKey();
                    BigDecimal bp = Optional.ofNullable(bestPd.get().getValue()).orElse(BigDecimal.ZERO);
                    finalPercent = clampPercent(bp.setScale(0, RM).intValue());
                } else {
                    Optional<Map.Entry<DiscountCampaign, BigDecimal>> bestP = activeCandidates.stream()
                            .filter(c -> appliesToProduct.test(c, productId))
                            .map(c -> Map.entry(c, Optional.ofNullable(c.getDiscountPercentage()).orElse(BigDecimal.ZERO)))
                            .max(Comparator.comparing(e -> e.getValue(), Comparator.nullsFirst(BigDecimal::compareTo)));

                    if (bestP.isPresent()) {
                        chosen = bestP.get().getKey();
                        BigDecimal bp = Optional.ofNullable(bestP.get().getValue()).orElse(BigDecimal.ZERO);
                        finalPercent = clampPercent(bp.setScale(0, RM).intValue());
                    }
                }
            }

            // 3) Áp/huỷ campaign tại dòng
            if (finalPercent == 0) {
                // Không có campaign áp dụng
                if (oldCampaign != null) {
                    // Trước đây có, giờ không còn → gỡ và báo
                    anyCampaignDetached = true;
                    String name = (pd.getProduct() != null && pd.getProduct().getProductName() != null)
                            ? pd.getProduct().getProductName() : "Sản phẩm";
                    affectedProducts.add(name);
                }
                d.setDiscountCampaign(null);
            } else {
                d.setDiscountCampaign(chosen);
            }

            // 4) Tính giá sau giảm
            BigDecimal discountAmt = basePrice.multiply(BigDecimal.valueOf(finalPercent))
                    .divide(BigDecimal.valueOf(100), MONEY_SCALE, RM);
            BigDecimal discounted = money(basePrice.subtract(discountAmt));
            if (discounted.compareTo(BigDecimal.ZERO) < 0) discounted = BigDecimal.ZERO;

            d.setSellPrice(basePrice);
            d.setDiscountPercentage(finalPercent);
            d.setDiscountedPrice(discounted);
            d.setUpdatedDate(now);
            d.setUpdatedBy(username);

            invoiceDetailRepository.save(d);
        }

        // Nếu có ít nhất 1 dòng bị gỡ campaign → ném exception để FE bắn toast
        if (anyCampaignDetached) {
            throw DiscountCampaignInvalidException.removed(affectedProducts);
        }
    }

    @Transactional
    public void updateInvoiceTotal(Invoice invoice) {
        List<InvoiceDetail> details = invoiceDetailRepository.findByInvoiceAndStatus(invoice, 1);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal productDiscount = BigDecimal.ZERO;
        BigDecimal subtotalAfterProduct = BigDecimal.ZERO;

        for (InvoiceDetail d : details) {
            BigDecimal qty = BigDecimal.valueOf(d.getQuantity());
            BigDecimal itemTotal = money(d.getSellPrice()).multiply(qty);
            BigDecimal discounted = money(d.getDiscountedPrice()).multiply(qty);

            totalAmount = totalAmount.add(itemTotal);
            productDiscount = productDiscount.add(itemTotal.subtract(discounted));
            subtotalAfterProduct = subtotalAfterProduct.add(discounted);
        }

        // === Re-validate voucher mỗi lần tính ===
        LocalDateTime now = LocalDateTime.now();
        Voucher v = invoice.getVoucher();

        VoucherValidity validity = getVoucherValidity(v, now); // helper dưới
        boolean voucherJustDetached = false;

        if (v != null && validity != VoucherValidity.VALID) {
            // gỡ voucher ra khỏi hóa đơn
            invoice.setVoucher(null);
            v = null;
            voucherJustDetached = true;
        }

        BigDecimal voucherDiscount = BigDecimal.ZERO;
        if (v != null) {
            voucherDiscount = calculateVoucherDiscountForAmount(subtotalAfterProduct, v);
            if (voucherDiscount.compareTo(subtotalAfterProduct) > 0) voucherDiscount = subtotalAfterProduct;
        }

        BigDecimal shipping = money(invoice.getShippingFee());
        BigDecimal totalDiscount = productDiscount.add(voucherDiscount);
        BigDecimal finalAmount = subtotalAfterProduct.subtract(voucherDiscount).add(shipping);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;

        invoice.setTotalAmount(money(totalAmount));
        invoice.setDiscountAmount(money(totalDiscount));
        invoice.setFinalAmount(money(finalAmount));
        invoice.setUpdatedDate(new Date());              // entity Invoice dùng Date → giữ nguyên
        invoice.setUpdatedBy(currentUsername());
        invoiceRepository.save(invoice);

        // === BẮN THÔNG BÁO cho POS nếu vừa gỡ voucher ===
        if (voucherJustDetached) {
            switch (validity) {
                case REMOVED_OR_DISABLED -> { throw VoucherInvalidException.removed(); }
                case EXPIRED -> { throw VoucherInvalidException.expired(); }
                case NOT_YET_ACTIVE -> { throw VoucherInvalidException.notYetActive(); }
                default -> { throw new VoucherInvalidException("VOUCHER_INVALID",
                        "Voucher không còn hợp lệ, hệ thống đã tự bỏ voucher khỏi hoá đơn."); }
            }
        }
    }

    private enum VoucherValidity { VALID, REMOVED_OR_DISABLED, NOT_YET_ACTIVE, EXPIRED }

    private VoucherValidity getVoucherValidity(Voucher v, LocalDateTime now) {
        if (v == null) return VoucherValidity.VALID; // không có voucher -> coi như hợp lệ (không báo)
        Integer status = v.getStatus();
        if (status == null || status != 1) return VoucherValidity.REMOVED_OR_DISABLED;

        // --- thời gian hiệu lực ---
        LocalDateTime start = v.getStartDate(); // sửa đúng kiểu field của bạn
        LocalDateTime end   = v.getEndDate();

        if (start != null && now.isBefore(start)) return VoucherValidity.NOT_YET_ACTIVE;
        if (end != null && now.isAfter(end))     return VoucherValidity.EXPIRED;

        return VoucherValidity.VALID;
    }

    private BigDecimal calculateVoucherDiscountForAmount(BigDecimal base, Voucher voucher) {
        if (voucher == null) return BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        if (voucher.getStatus() == null || voucher.getStatus() != 1) return BigDecimal.ZERO;
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) return BigDecimal.ZERO;
        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) return BigDecimal.ZERO;

        BigDecimal minOrder = money(voucher.getMinOrderValue());
        BigDecimal amount = money(base);
        if (amount.compareTo(minOrder) < 0) return BigDecimal.ZERO;

        BigDecimal discount = BigDecimal.ZERO;
        if (voucher.getDiscountPercentage() != null && voucher.getDiscountPercentage().compareTo(BigDecimal.ZERO) > 0) {
            discount = amount.multiply(voucher.getDiscountPercentage())
                    .divide(BigDecimal.valueOf(100), MONEY_SCALE, RM);
            BigDecimal cap = money(voucher.getMaxDiscountValue());
            if (cap.compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(cap) > 0) discount = cap;
        } else if (voucher.getDiscountAmount() != null && voucher.getDiscountAmount() > 0) {
            discount = money(BigDecimal.valueOf(voucher.getDiscountAmount()));
        }
        return money(discount);
    }

    @Override
    public List<CustomerResponse> findCustomersByPhonePrefix(String phonePrefix) {
        if (phonePrefix == null || phonePrefix.isBlank()) return Collections.emptyList();
        return customerRepository.findByPhoneStartingWithAndStatusActive(phonePrefix)
                .stream().map(invoiceMapper::toCustomerResponse).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public CustomerResponse createQuickCustomer(String phone, String name, String email) {
        // ===== Validate input =====
        if (phone == null || phone.isBlank()) {
            throw new RuntimeException("Số điện thoại không được trống");
        }

        final String normalizedPhone = phone.trim();
        final String normalizedName  = (name == null || name.isBlank()) ? "Khách lẻ" : name.trim();
        final String normalizedEmail = (email == null || email.isBlank()) ? null : email.trim();
        final String emailLc         = (normalizedEmail == null) ? null : normalizedEmail.toLowerCase(); // chuẩn hoá email
        final LocalDateTime now = LocalDateTime.now();

        // ===== 1) Kiểm tra trùng SĐT =====
        if (customerRepository.findTop1ByPhoneAndStatus(normalizedPhone, 1).isPresent()) {
            throw new RuntimeException("Số điện thoại đã tồn tại");
        }

        // ===== 2) Kiểm tra trùng Email =====
        if (emailLc != null) {
            // nếu có thể, dùng findTop1ByEmailAndStatusIgnoreCase
            if (customerRepository.findTop1ByEmailAndStatus(emailLc, 1).isPresent()) {
                throw new RuntimeException("Email đã tồn tại");
            }
        }

        // ===== 3) Tạo customer mới =====
        Customer customer = new Customer();
        customer.setCustomerName(normalizedName);
        customer.setPhone(normalizedPhone);
        customer.setEmail(emailLc);
        customer.setCustomerCode("CUS-" + System.currentTimeMillis());
        customer.setStatus(1);
        customer.setCreatedDate(now);
        customer.setCreatedBy(currentUsername());
        customer = customerRepository.save(customer);

        // ===== 4) Nếu có email → auto tạo User =====
        if (emailLc != null) {
            // Chỉ query 1 lần
            Optional<User> userOpt = userRepository.findByUsername(emailLc); // nếu có thể: findByUsernameIgnoreCase
            if (userOpt.isEmpty()) {
                // Tạo user mới (giữ hàm 2 tham số như bạn yêu cầu)
                createUserForCustomer(customer, emailLc);

                // Lấy user vừa tạo để set mật khẩu & phát event gửi mail NỀN
                userRepository.findByUsername(emailLc).ifPresent(newUser -> {
                    // Sinh mật khẩu 6 ký tự
                    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
                    SecureRandom rnd = new SecureRandom();
                    StringBuilder pw = new StringBuilder();
                    for (int i = 0; i < 6; i++) pw.append(chars.charAt(rnd.nextInt(chars.length())));
                    String rawPassword = pw.toString();

                    // Cập nhật mật khẩu (mã hoá)
                    newUser.setPassword(passwordEncoder.encode(rawPassword));
                    newUser.setUpdatedAt(new Date());
                    newUser.setUpdatedBy(currentUsername());
                    userRepository.save(newUser);

//                     Nếu bạn chưa tạo listener, có thể gọi trực tiếp service đã @Async:
                    accountEmailService.sendAccountCreatedEmail(emailLc, normalizedName, newUser.getUsername(), rawPassword);
                });
            } else {
                User u = userOpt.get();
                if (u.getCustomer() == null) {
                    u.setCustomer(customer);
                    u.setUpdatedAt(new Date());
                    u.setUpdatedBy(currentUsername());
                    userRepository.save(u);
                }
            }
        }

        // ===== 5) Trả về DTO =====
        return invoiceMapper.toCustomerResponse(customer);
    }

    private void createUserForCustomer(Customer customer, String emailAsUsername) {
        String rawPassword = generateRandomPassword(10);
        String hashed = passwordEncoder.encode(rawPassword);

        User u = new User();
        u.setUsername(emailAsUsername);
        u.setPassword(hashed);
        u.setCreatedAt(new Date());           // User.createdAt là java.util.Date
        u.setCreatedBy(currentUsername());
        u.setRole(3);                         // ví dụ: 3 = CUSTOMER, tuỳ hệ thống quyền của bạn
        u.setCustomer(customer);

        userRepository.save(u);

        // TODO: Gửi email thông báo mật khẩu tạm cho khách hoặc phát kênh khác.
        // KHÔNG trả mật khẩu thô qua API response.
    }

    private String generateRandomPassword(int length) {
        final String dict = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%!";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(dict.charAt(rnd.nextInt(dict.length())));
        }
        return sb.toString();
    }

    @Transactional
    public void assignCustomer(Long invoiceId, Long customerId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn với id: " + invoiceId));

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với id: " + customerId));

        // Kiểm tra nếu khách hàng đã được gán vào hóa đơn khác có status = 0
        boolean isCustomerAssigned = invoiceRepository.existsByCustomer_IdAndStatusAndOrderType(
                customerId,
                TrangThaiTong.DANG_XU_LY,
                0
        );

        if (isCustomerAssigned) {
            throw new RuntimeException("Khách hàng đã có một hóa đơn đang xử lý");
        }

        // Gán khách hàng vào hóa đơn
        invoice.setCustomer(customer);

        // Gán thêm số điện thoại khách hàng vào invoice.phone
        invoice.setPhone(customer.getPhone());

        invoiceRepository.save(invoice);
    }

    @Override
    public PaymentSummary calculatePayment(Long invoiceId, BigDecimal amountGiven) {
        if (amountGiven == null || amountGiven.compareTo(BigDecimal.ZERO)<0)
            throw new RuntimeException("Số tiền đưa không hợp lệ");

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại"));

        applyDiscountToInvoiceDetails(invoice);
        updateInvoiceTotal(invoice);

        BigDecimal finalAmount = money(invoice.getFinalAmount());
        if (amountGiven.compareTo(finalAmount) < 0) throw new RuntimeException("Số tiền đưa không đủ");

        BigDecimal change = amountGiven.subtract(finalAmount);
        return new PaymentSummary(finalAmount, amountGiven, money(change));
    }

    @Transactional
    public void cancelInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại"));

        // Giải phóng giữ chỗ
        releaseOldHolds(invoice);

        // Xóa chi tiết
        List<InvoiceDetail> invoiceDetails = invoiceDetailRepository.findByInvoiceAndStatus(invoice, 1);
        invoiceDetailRepository.deleteAll(invoiceDetails);

        // Bỏ voucher + reset
        invoice.setVoucher(null);
        invoice.setStatus(TrangThaiTong.DA_HUY);
        invoice.setTotalAmount(BigDecimal.ZERO);
        invoice.setFinalAmount(BigDecimal.ZERO);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setUpdatedDate(new Date());
        invoiceRepository.save(invoice);
    }

    @Transactional
    @Override
    public void checkout(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn với ID: " + invoiceId));

        if (invoice.getStatus() != TrangThaiTong.DANG_XU_LY) {
            throw new RuntimeException("Chỉ checkout khi hóa đơn đang xử lý");
        }

        // 0) Validate voucher (nếu có)
        Instant nowInstant = Instant.now();
        validateVoucherIfAny(invoice, nowInstant);

        // 1) Re-calc giá/giảm
        applyDiscountToInvoiceDetails(invoice);
        updateInvoiceTotal(invoice);

        String username = currentUsername();
        LocalDateTime now = LocalDateTime.now();

        // 2) Trừ kho + kiểm tra ngừng bán/hết hàng
        List<InvoiceDetail> details = invoiceDetailRepository.findByInvoiceAndStatus(invoice, 1);
        for (InvoiceDetail d : details) {
            ProductDetail pd = productDetailRepository.findByIdAndStatus(d.getProductDetail().getId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm đã bị xoá hoặc không tồn tại."));

            String name = pd.getProduct() != null ? pd.getProduct().getProductName() : "Sản phẩm";

            // --- NGỪNG BÁN ---
            boolean productInactive = pd.getProduct() != null
                    && pd.getProduct().getStatus() != null
                    && pd.getProduct().getStatus() != 1;
            boolean detailInactive = pd.getStatus() != null && pd.getStatus() != 1;

            if (productInactive || detailInactive) {
                throw new RuntimeException(name + " đã ngừng bán. Vui lòng xoá khỏi giỏ hàng.");
            }

            // --- TỒN KHO ---
            int stock = pd.getQuantity() == null ? 0 : pd.getQuantity();
            if (stock <= 0 || stock < d.getQuantity()) {
                throw new RuntimeException(name + " đã hết hàng hoặc không đủ tồn kho. Vui lòng xoá khỏi giỏ hàng.");
            }

            // --- Trừ kho ---
            pd.setQuantity(stock - d.getQuantity());
            pd.setUpdatedDate(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()));
            pd.setUpdatedBy(username);
            productDetailRepository.save(pd);
        }

        // 3) Đánh dấu thanh toán thành công
        invoice.setStatus(TrangThaiTong.THANH_CONG);
        invoice.setIsPaid(true);
        invoice.setUpdatedBy(username);
        invoice.setUpdatedDate(new Date());
        invoiceRepository.save(invoice);

        // 4) Ghi nhận voucher đã dùng
        markVoucherUsedIfAny(invoice, now);

        // 5) (Tuỳ) Tặng voucher khuyến khích sau đơn
        handleAutoPromoVoucher(invoice, username, now);
    }

    private void validateVoucherIfAny(Invoice invoice, Instant now) {
        Voucher v = invoice.getVoucher();
        if (v == null) return;

        Integer status = v.getStatus();
        if (status == null || status != 1) {
            throw new RuntimeException("Voucher không tồn tại.");
        }

        LocalDateTime start = v.getStartDate(); // LocalDateTime
        LocalDateTime end   = v.getEndDate();   // LocalDateTime

        ZoneId zone = ZoneId.systemDefault();

        if (start != null && now.isBefore(start.atZone(zone).toInstant())) {
            throw new RuntimeException("Voucher chưa đến thời gian áp dụng.");
        }
        if (end != null && now.isAfter(end.atZone(zone).toInstant())) {
            throw new RuntimeException("Voucher đã hết hạn.");
        }
    }

    private void handleAutoPromoVoucher(Invoice invoice, String username, LocalDateTime now) {
        // Điều kiện tiên quyết
        if (invoice == null || invoice.getCustomer() == null || invoice.getTotalAmount() == null) return;

        // ✅ Chỉ tặng khi trạng thái tổng = THANH_CONG
        if (invoice.getStatus() != TrangThaiTong.THANH_CONG) return;

        BigDecimal totalAmount = invoice.getTotalAmount();
        Long customerId = invoice.getCustomer().getId();

        // Lấy các voucher đang hoạt động + còn hạn + có minOrderToReceive
        List<Voucher> activePromos = voucherRepository.findByStatus(1).stream()
                .filter(v -> {
                    LocalDateTime start = v.getStartDate();
                    LocalDateTime end   = v.getEndDate();
                    boolean timeOk = (start == null || !now.isBefore(start)) &&
                            (end   == null || !now.isAfter(end));
                    return timeOk && v.getMinOrderToReceive() != null;
                })
                .collect(Collectors.toList());

        // Chọn ngưỡng cao nhất thỏa điều kiện
        Voucher matchedPromo = activePromos.stream()
                .filter(v -> totalAmount.compareTo(v.getMinOrderToReceive()) >= 0)
                .sorted((v1, v2) -> v2.getMinOrderToReceive().compareTo(v1.getMinOrderToReceive()))
                .findFirst()
                .orElse(null);

        if (matchedPromo == null) return;

        int discountAmount = matchedPromo.getDiscountAmount() != null ? matchedPromo.getDiscountAmount() : 0;

        // Tránh tặng trùng
        boolean alreadyGiven = voucherRepository.existsByCustomerIdAndVoucherNameAndDiscountAmount(
                customerId, matchedPromo.getVoucherName(), discountAmount
        );
        if (alreadyGiven) return;

        // Tạo voucher tặng
        Voucher newVoucher = new Voucher();
        newVoucher.setCustomer(invoice.getCustomer());
        newVoucher.setVoucherCode("VOUCHER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        newVoucher.setVoucherName(matchedPromo.getVoucherName());
        newVoucher.setDiscountAmount(discountAmount);

        // Điều kiện áp dụng (copy, có thể chỉnh về ZERO nếu muốn không yêu cầu tối thiểu)
        newVoucher.setDiscountPercentage(
                matchedPromo.getDiscountPercentage() != null ? matchedPromo.getDiscountPercentage() : BigDecimal.ZERO
        );
        newVoucher.setMinOrderValue(
                matchedPromo.getMinOrderValue() != null ? matchedPromo.getMinOrderValue() : BigDecimal.ZERO
        );
        newVoucher.setMaxDiscountValue(
                matchedPromo.getMaxDiscountValue() != null ? matchedPromo.getMaxDiscountValue() : BigDecimal.ZERO
        );

        // Hạn dùng 30 ngày
        newVoucher.setStartDate(now);
        newVoucher.setEndDate(now.plusDays(30));
        newVoucher.setStatus(1);
        newVoucher.setCreatedDate(now);
        newVoucher.setCreatedBy(username != null ? username : "SYSTEM");
        newVoucher.setQuantity(1);
        newVoucher.setVoucherType(0); // loại tặng
        newVoucher.setOrderType(1);   // tùy quy ước (online?)

        voucherRepository.save(newVoucher);

        // Gửi email
        String email = invoice.getCustomer().getEmail();
        if (email != null && !email.isEmpty()) {
            voucherEmailService.sendVoucherNotificationEmail(
                    email,
                    invoice.getCustomer().getCustomerName(),
                    totalAmount,
                    BigDecimal.valueOf(discountAmount),
                    newVoucher.getDiscountPercentage(),
                    newVoucher.getVoucherCode(),
                    newVoucher.getEndDate().toLocalDate()
            );
        }
    }

    @Transactional
    @Override
    public void clearCart(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        final boolean RESERVE_STOCK_ON_ADD_POS = false; // nếu bạn có giữ kho khi add giỏ POS thì set true

        if (RESERVE_STOCK_ON_ADD_POS) {
            for (InvoiceDetail d : invoice.getInvoiceDetails()) {
                if (d.getStatus()!=null && d.getStatus()!=1) continue;
                ProductDetail pd = d.getProductDetail();
                pd.setQuantity((pd.getQuantity()==null?0:pd.getQuantity()) + d.getQuantity());
                productDetailRepository.save(pd);
            }
        }

        for (InvoiceDetail d : invoice.getInvoiceDetails()) d.setStatus(2);
        invoiceDetailRepository.saveAll(invoice.getInvoiceDetails());
        invoice.getInvoiceDetails().clear();

        invoice.setTotalAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RM));
        invoice.setDiscountAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RM));
        invoice.setFinalAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, RM));
        invoice.setUpdatedDate(new Date());
        invoice.setUpdatedBy(currentUsername());
        invoiceRepository.save(invoice);
    }

    @Transactional
    public void deleteCartItemById(Long invoiceDetailId) {
        if (invoiceDetailId == null) {
            throw new IllegalArgumentException("Thiếu invoiceDetailId");
        }

        // Nếu bạn có khoá ghi, dùng findByIdForUpdate để tránh race
        InvoiceDetail detail = invoiceDetailRepository.findById(invoiceDetailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ"));

        Invoice invoice = detail.getInvoice();
        if (invoice == null) {
            throw new RuntimeException("Hóa đơn không tồn tại");
        }

        // (Tuỳ chọn) nếu bạn có cơ chế giữ kho khi thêm vào giỏ, hoàn kho khi xóa:
        final boolean RESERVE_STOCK_ON_ADD = false; // đặt true nếu bạn đã trừ kho khi add-to-cart
        if (RESERVE_STOCK_ON_ADD && detail.getStatus() != null && detail.getStatus() == 1) {
            ProductDetail pd = detail.getProductDetail();
            pd.setQuantity((pd.getQuantity() == null ? 0 : pd.getQuantity()) + detail.getQuantity());
            productDetailRepository.save(pd);
        }

        // Xóa mềm dòng giỏ hàng
        detail.setStatus(2);
        detail.setUpdatedDate(LocalDateTime.now());
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        detail.setUpdatedBy(username);
        invoiceDetailRepository.save(detail);

        // Lấy lại các dòng còn active
        List<InvoiceDetail> activeDetails = invoiceDetailRepository.findByInvoiceAndStatus(invoice, 1);

        if (activeDetails.isEmpty()) {
            // Giỏ trống: reset toàn bộ số tiền
            invoice.setTotalAmount(BigDecimal.ZERO);
            invoice.setDiscountAmount(BigDecimal.ZERO);
            invoice.setFinalAmount(BigDecimal.ZERO);

            // Nếu có voucher/chiến dịch áp toàn hoá đơn thì có thể clear ở đây (nếu có field)
            // invoice.setVoucher(null);
            // invoice.setDiscountCampaign(null);

            invoiceRepository.save(invoice);
            return;
        }

        // Áp lại giảm giá từng dòng (nếu có logic tự tìm campaign/% giảm tốt nhất)
        applyDiscountToInvoiceDetails(invoice);

        // Tính lại tổng tiền, số tiền giảm & số tiền phải trả (gồm cả giảm giá cấp hoá đơn nếu có)
        updateInvoiceTotal(invoice);

        invoiceRepository.save(invoice);
    }

    @Transactional
    public InvoiceDisplayResponse addInvoiceDetails(
            Long invoiceId,
            Long productDetailId,
            Integer quantity,
            Integer discountPercentage /* ignored */,
            Long discountCampaignId
    ) {
        // ===== Validate =====
        if (invoiceId == null || productDetailId == null) {
            throw new RuntimeException("Thiếu id");
        }
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Số lượng phải > 0");
        }

        // ===== Invoice & state =====
        final Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại"));
        if (invoice.getStatus() != TrangThaiTong.DANG_XU_LY) {
            throw new RuntimeException("Chỉ thêm sản phẩm khi hóa đơn đang xử lý");
        }

        // ===== ProductDetail (nếu bị xoá hard -> ném lỗi) =====
        final ProductDetail pd = productDetailRepository.findById(productDetailId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm/biến thể đã bị xóa hoặc không tồn tại"));

        // ===== CHẶN NGAY: 'ngừng bán' / 'hết hàng' =====
        final String prodName = (pd.getProduct() != null && pd.getProduct().getProductName() != null)
                ? pd.getProduct().getProductName() : "Sản phẩm";

        // Ngừng bán theo status (chỉ dùng status, không có isDeleted)
        final boolean productInactive = pd.getProduct() != null
                && pd.getProduct().getStatus() != null
                && pd.getProduct().getStatus() != 1;
        final boolean detailInactive = pd.getStatus() != null && pd.getStatus() != 1;
        if (productInactive || detailInactive) {
            throw new RuntimeException(prodName + " đã ngừng bán. Vui lòng xoá khỏi giỏ hàng.");
        }

        // Hết hàng
        final int available = pd.getQuantity() == null ? 0 : pd.getQuantity();
        if (available <= 0) {
            throw new RuntimeException(prodName + " đã hết hàng. Vui lòng xoá khỏi giỏ hàng.");
        }

        // ===== Lấy (nếu có) dòng hiện tại để kiểm tra tổng =====
        InvoiceDetail line = invoiceDetailRepository.findByInvoiceAndProductDetail(invoice, pd).orElse(null);
        final int existingQty = (line != null && !Objects.equals(line.getStatus(), 2))
                ? (line.getQuantity() == null ? 0 : line.getQuantity())
                : 0;

        // Không trừ kho khi add -> tổng yêu cầu không được vượt tồn
        final int requestedTotal = existingQty + quantity;
        if (available < requestedTotal) {
            throw new RuntimeException("Số lượng trong kho không đủ cho " + prodName + ". Hiện còn: " + available);
        }

        // ===== Constants / context =====
        final int SCALE = 2;
        final RoundingMode RM = RoundingMode.HALF_UP;
        final BigDecimal ONE_HUNDRED = new BigDecimal("100");
        final LocalDateTime now = LocalDateTime.now();
        final String username = currentUsername();
        final Long pdIdFinal = productDetailId;
        final Long productIdFinal = pd.getProduct().getId();

        // Giá gốc theo SPCT
        final BigDecimal sellPrice = Optional.ofNullable(pd.getSellPrice())
                .orElse(BigDecimal.ZERO)
                .setScale(SCALE, RM);

        // ===== Active campaigns =====
        final List<DiscountCampaign> active = discountCampaignRepository.findActiveCampaigns(now);
        final java.util.function.Predicate<DiscountCampaign> isActive = c ->
                (c.getStartDate() == null || !now.isBefore(c.getStartDate())) &&
                        (c.getEndDate() == null || !now.isAfter(c.getEndDate()));

        // ===== Chọn % giảm tốt nhất =====
        BigDecimal bestPercent = BigDecimal.ZERO;
        Long bestCampaignId = null;

        // 1) FE gửi sẵn campaignId
        if (discountCampaignId != null) {
            final DiscountCampaign tmp = discountCampaignRepository.findById(discountCampaignId)
                    .orElseThrow(() -> new RuntimeException("Chiến dịch giảm giá không tồn tại"));
            if (isActive.test(tmp)) {
                final boolean appliesPd = tmp.getProductDetails() != null &&
                        tmp.getProductDetails().stream().anyMatch(link ->
                                link.getProductDetail() != null &&
                                        Objects.equals(link.getProductDetail().getId(), pdIdFinal));
                final boolean appliesP = !appliesPd && tmp.getProducts() != null &&
                        tmp.getProducts().stream().anyMatch(link ->
                                link.getProduct() != null &&
                                        Objects.equals(link.getProduct().getId(), productIdFinal));
                if (appliesPd || appliesP) {
                    final Optional<BigDecimal> pctOnPdLink = Optional.ofNullable(tmp.getProductDetails())
                            .orElseGet(java.util.Collections::emptyList).stream()
                            .filter(link -> link.getProductDetail() != null &&
                                    Objects.equals(link.getProductDetail().getId(), pdIdFinal))
                            .map(DiscountCampaignProductDetail::getDiscountPercentage)
                            .filter(Objects::nonNull)
                            .findFirst();

                    bestPercent = pctOnPdLink.orElse(Optional.ofNullable(tmp.getDiscountPercentage())
                            .orElse(BigDecimal.ZERO));
                    bestCampaignId = tmp.getId();
                }
            }
        }

        // 2) Tự tìm best nếu chưa chốt
        if (bestCampaignId == null) {
            final Optional<AbstractMap.SimpleEntry<Long, BigDecimal>> pdBest = active.stream()
                    .filter(isActive)
                    .filter(c -> c.getProductDetails() != null)
                    .flatMap(c -> c.getProductDetails().stream()
                            .filter(link -> link.getProductDetail() != null &&
                                    Objects.equals(link.getProductDetail().getId(), pdIdFinal))
                            .map(link -> new AbstractMap.SimpleEntry<>(
                                    c.getId(),
                                    link.getDiscountPercentage() != null ? link.getDiscountPercentage()
                                            : Optional.ofNullable(c.getDiscountPercentage()).orElse(BigDecimal.ZERO)
                            )))
                    .max(Comparator.comparing(Map.Entry::getValue, Comparator.nullsFirst(BigDecimal::compareTo)));

            if (pdBest.isPresent()) {
                bestCampaignId = pdBest.get().getKey();
                bestPercent    = pdBest.get().getValue();
            } else {
                final Optional<AbstractMap.SimpleEntry<Long, BigDecimal>> pBest = active.stream()
                        .filter(isActive)
                        .filter(c -> c.getProducts() != null)
                        .flatMap(c -> c.getProducts().stream()
                                .filter(link -> link.getProduct() != null &&
                                        Objects.equals(link.getProduct().getId(), productIdFinal))
                                .map(link -> new AbstractMap.SimpleEntry<>(
                                        c.getId(),
                                        Optional.ofNullable(c.getDiscountPercentage()).orElse(BigDecimal.ZERO)
                                )))
                        .max(Comparator.comparing(Map.Entry::getValue, Comparator.nullsFirst(BigDecimal::compareTo)));

                if (pBest.isPresent()) {
                    bestCampaignId = pBest.get().getKey();
                    bestPercent    = pBest.get().getValue();
                }
            }
        }

        if (bestPercent == null) bestPercent = BigDecimal.ZERO;
        final int percentInt = Math.max(0, Math.min(bestPercent.setScale(0, RM).intValue(), 100));

        // ===== Tính giá sau giảm =====
        final BigDecimal discountAmount = sellPrice.multiply(BigDecimal.valueOf(percentInt))
                .divide(ONE_HUNDRED, SCALE, RM);
        final BigDecimal discountedPrice = sellPrice.subtract(discountAmount)
                .max(BigDecimal.ZERO).setScale(SCALE, RM);

        // ===== Cập nhật / gộp dòng HĐ (không trừ kho ở đây) =====
        final Long finalBestCampaignId = bestCampaignId;

        if (line != null) {
            if (Objects.equals(line.getStatus(), 2)) {
                line.setStatus(1);
                line.setQuantity(quantity);
            } else {
                line.setQuantity(requestedTotal); // dùng tổng đã kiểm tra ở trên
            }
            line.setSellPrice(sellPrice);
            line.setDiscountPercentage(percentInt);
            line.setDiscountedPrice(discountedPrice);

            if (finalBestCampaignId != null) {
                final DiscountCampaign dc = active.stream()
                        .filter(c -> Objects.equals(c.getId(), finalBestCampaignId))
                        .findFirst()
                        .orElse(null);
                line.setDiscountCampaign(dc);
            } else {
                line.setDiscountCampaign(null);
            }

            line.setUpdatedDate(now);
            line.setUpdatedBy(username);
        } else {
            line = new InvoiceDetail();
            line.setInvoice(invoice);
            line.setProductDetail(pd);
            line.setQuantity(quantity);
            line.setSellPrice(sellPrice);
            line.setDiscountPercentage(percentInt);
            line.setDiscountedPrice(discountedPrice);

            if (finalBestCampaignId != null) {
                final DiscountCampaign dc = active.stream()
                        .filter(c -> Objects.equals(c.getId(), finalBestCampaignId))
                        .findFirst()
                        .orElse(null);
                line.setDiscountCampaign(dc);
            }

            line.setStatus(1);
            line.setCreatedDate(now);
            line.setCreatedBy(username);
            line.setInvoiceCodeDetail("INV-D-" + invoice.getId() + "-" + (System.nanoTime() % 100000));
        }

        // ===== KHÔNG trừ kho ở đây =====
        invoiceDetailRepository.saveAndFlush(line);

        // Không recalc toàn giỏ để tránh đè discount dòng
        updateInvoiceTotal(invoice);

        final List<InvoiceDetail> all = invoiceDetailRepository.findByInvoiceAndStatus(invoice, 1);
        return invoiceMapper.toInvoiceDisplayResponse(invoice, all);
    }

    @Transactional
    public InvoiceDisplayResponse updateInvoiceDetailQuantity(Long invoiceDetailId, Integer newQuantity) {
        if (newQuantity == null || newQuantity <= 0) throw new RuntimeException("Số lượng phải > 0");

        InvoiceDetail line = invoiceDetailRepository.findById(invoiceDetailId)
                .orElseThrow(() -> new RuntimeException("Chi tiết hóa đơn không tồn tại"));

        ProductDetail pd = line.getProductDetail();
        int available = pd.getQuantity() == null ? 0 : pd.getQuantity();
        int delta = newQuantity - line.getQuantity();
        if (delta > 0 && available < delta) throw new RuntimeException("Số lượng trong kho không đủ");

        line.setQuantity(newQuantity);
        line.setUpdatedDate(LocalDateTime.now());
        line.setUpdatedBy(currentUsername());
        invoiceDetailRepository.save(line);

        Invoice invoice = line.getInvoice();
        applyDiscountToInvoiceDetails(invoice);
        updateInvoiceTotal(invoice);

        List<InvoiceDetail> all = invoiceDetailRepository.findByInvoiceAndStatus(invoice, 1);
        return invoiceMapper.toInvoiceDisplayResponse(invoice, all);
    }

    @Transactional
    public Invoice applyVoucherToInvoice(Long invoiceId, String voucherCode) {
        if (voucherCode == null || voucherCode.isBlank())
            throw new RuntimeException("Mã voucher không được để trống");

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        if (invoice.getCustomer() == null) {
            throw new RuntimeException("Vui lòng chọn khách hàng trước khi áp voucher");
        }

        Voucher voucher = voucherRepository.findByVoucherCode(voucherCode.trim())
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStatus() == null || voucher.getStatus() != 1) throw new RuntimeException("Voucher không khả dụng");
        if (voucher.getStartDate()!=null && now.isBefore(voucher.getStartDate())) throw new RuntimeException("Voucher chưa đến thời gian áp dụng");
        if (voucher.getEndDate()!=null && now.isAfter(voucher.getEndDate())) throw new RuntimeException("Voucher đã hết hạn");
        if (voucher.getCustomer()!=null
                && !voucher.getCustomer().getId().equals(invoice.getCustomer().getId()))
            throw new RuntimeException("Voucher không áp dụng cho khách hàng này");

        // Tính subtotal sau giảm SP để check min
        applyDiscountToInvoiceDetails(invoice);
        updateInvoiceTotal(invoice);

        List<InvoiceDetail> details = invoiceDetailRepository.findByInvoiceAndStatus(invoice, 1);
        BigDecimal subtotalAfterProduct = details.stream()
                .map(d -> money(d.getDiscountedPrice()).multiply(BigDecimal.valueOf(d.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal minOrderValue = voucher.getMinOrderValue() != null ? money(voucher.getMinOrderValue()) : BigDecimal.ZERO;
        if (subtotalAfterProduct.compareTo(minOrderValue) < 0) {
            throw new RuntimeException("Số tiền tối thiểu để áp dụng voucher là " + minOrderValue + "đ");
        }

        // ❗ Quy tắc "1 mã/khách": KH đã dùng mã này trước đó?
        boolean usedElsewhere = voucherHistoryRepository
                .existsByVoucherAndCustomerAndStatusAndInvoiceNot(
                        voucher, invoice.getCustomer(), 1, invoice);
        if (usedElsewhere) {
            throw new RuntimeException("Khách hàng đã sử dụng mã voucher này trước đó.");
        }

        // Cho phép switch voucher trên cùng invoice → dọn hold cũ rồi giữ chỗ mới
        releaseOldHolds(invoice);

        invoice.setVoucher(voucher);
        updateInvoiceTotal(invoice);

        BigDecimal voucherDiscount = calculateVoucherDiscountForAmount(subtotalAfterProduct, voucher);

        VoucherHistory hist = new VoucherHistory();
        hist.setVoucher(voucher);
        hist.setInvoice(invoice);
        hist.setCustomer(invoice.getCustomer());
        hist.setUsedAt(now);
        hist.setDiscountValueApplied(voucherDiscount);
        hist.setStatus(0); // giữ chỗ
        voucherHistoryRepository.save(hist);

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice applyBestVoucherToInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn với ID: " + invoiceId));

        // 1) Lấy các dòng hợp lệ (status = 1, qty > 0)
        List<InvoiceDetail> details = invoiceDetailRepository.findByInvoiceAndStatus(invoice, 1);
        if (details == null || details.isEmpty()) {
            // Không có dòng → không áp voucher
            releaseOldHolds(invoice);
            invoice.setVoucher(null);
            updateInvoiceTotal(invoice);
            return invoiceRepository.save(invoice);
        }

        // 2) Áp khuyến mãi theo SPCT (nếu có) rồi tính tổng gốc trước voucher
        applyDiscountToInvoiceDetails(invoice);
        updateInvoiceTotal(invoice);

        Customer customer = invoice.getCustomer();
        if (customer == null) {
            releaseOldHolds(invoice);
            invoice.setVoucher(null);
            updateInvoiceTotal(invoice);
            return invoiceRepository.save(invoice);
        }

        // 3) Tính base = tổng sau chiết khấu dòng (nếu có), trước voucher
        BigDecimal base = details.stream()
                .map(d -> money(d.getDiscountedPrice()).multiply(BigDecimal.valueOf(d.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (base.compareTo(BigDecimal.ZERO) <= 0) {
            releaseOldHolds(invoice);
            invoice.setVoucher(null);
            updateInvoiceTotal(invoice);
            return invoiceRepository.save(invoice);
        }

        // 4) Thu thập productIds & categoryIds từ giỏ hiện tại (nếu có)
        Set<Long> productIds = new HashSet<>();
        Set<Long> categoryIds = new HashSet<>();

        for (InvoiceDetail d : details) {
            if (d == null || d.getProductDetail() == null) continue;
            Product p = d.getProductDetail().getProduct();
            if (p != null && p.getId() != null) {
                productIds.add(p.getId());
                // category của product (nếu có)
                List<ProductCategory> pcs = productCategoryRepository.findByProduct(p);
                if (pcs != null) {
                    for (ProductCategory pc : pcs) {
                        if (pc != null && pc.getCategory() != null && pc.getCategory().getId() != null) {
                            categoryIds.add(pc.getCategory().getId());
                        }
                    }
                }
            }
        }

        // 5) Cờ lọc an toàn: nếu không có ids thì KHÔNG ép lọc theo phần đó
        boolean hasProductIds  = !productIds.isEmpty();
        boolean hasCategoryIds = !categoryIds.isEmpty();
        boolean useProducts    = hasProductIds;      // chỉ lọc theo sản phẩm khi có productIds
        boolean useCategories  = hasCategoryIds;     // chỉ lọc theo danh mục khi có categoryIds

        // 6) Gọi query đã chỉnh: chỉ trả voucher đúng customerId; nương theo cờ lọc
        LocalDateTime now = LocalDateTime.now();
        Integer orderType = invoice.getOrderType();
        Long customerId = customer.getId();

        List<Voucher> eligible = voucherRepository.findValidVouchers(
                now,
                customerId,
                orderType,
                useProducts,
                useCategories,
                hasProductIds,
                hasCategoryIds,
                hasProductIds ? productIds : Collections.emptySet(),
                hasCategoryIds ? categoryIds : Collections.emptySet()
        );

        // 7) Loại voucher KH đã dùng (trừ giữ chỗ của chính invoice hiện tại nếu bạn đã xử lý trong repo)
        Set<Long> usedIds = voucherHistoryRepository
                .findVoucherIdsUsedByCustomerExcludingInvoice(customerId, invoiceId);

        List<Voucher> applicable = eligible.stream()
                .filter(v -> v != null && v.getId() != null && !usedIds.contains(v.getId()))
                .filter(v -> v.getQuantity() == null || v.getQuantity() > 0)
                // đủ minOrderValue so với base
                .filter(v -> v.getMinOrderValue() == null || base.compareTo(v.getMinOrderValue()) >= 0)
                .toList();

        // 8) Không có voucher hợp lệ → clear & lưu
        if (applicable.isEmpty()) {
            releaseOldHolds(invoice);
            invoice.setVoucher(null);
            updateInvoiceTotal(invoice);
            return invoiceRepository.save(invoice);
        }

        // 9) Chọn voucher tốt nhất theo số tiền giảm trên base
        Voucher best = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;
        for (Voucher v : applicable) {
            BigDecimal d = calculateVoucherDiscountForAmount(base, v);
            if (d.compareTo(bestDiscount) > 0) {
                best = v;
                bestDiscount = d;
            }
        }

        if (best == null) {
            releaseOldHolds(invoice);
            invoice.setVoucher(null);
            updateInvoiceTotal(invoice);
            return invoiceRepository.save(invoice);
        }

        // 10) Có best → clear hold cũ, set voucher mới, giữ chỗ
        releaseOldHolds(invoice);
        invoice.setVoucher(best);
        updateInvoiceTotal(invoice);

        VoucherHistory hold = new VoucherHistory();
        hold.setVoucher(best);
        hold.setInvoice(invoice);
        hold.setCustomer(customer);
        hold.setUsedAt(now);
        hold.setDiscountValueApplied(bestDiscount);
        hold.setStatus(0); // giữ chỗ
        voucherHistoryRepository.save(hold);

        return invoiceRepository.save(invoice);
    }

    private void releaseOldHolds(Invoice invoice) {
        List<VoucherHistory> olds = voucherHistoryRepository.findByInvoice(invoice);
        for (VoucherHistory h : olds) {
            if (h.getStatus() != null && h.getStatus() == 0) {
                h.setStatus(2); // hủy giữ chỗ
            }
        }
        voucherHistoryRepository.saveAll(olds);
    }

    private void markVoucherUsedIfAny(Invoice invoice, LocalDateTime now) {
        Voucher voucher = invoice.getVoucher();
        Customer customer = invoice.getCustomer();
        if (voucher == null || customer == null) return;

        boolean usedElsewhere = voucherHistoryRepository
                .existsByVoucherAndCustomerAndStatusAndInvoiceNot(voucher, customer, 1, invoice);
        if (usedElsewhere) {
            throw new RuntimeException("Voucher này đã được khách hàng sử dụng trước đó.");
        }

        VoucherHistory hold = voucherHistoryRepository
                .findTopByInvoiceAndVoucherAndStatus(invoice, voucher, 0)
                .orElse(null);

        if (hold != null) {
            hold.setStatus(1); // used
            hold.setUsedAt(now);
            voucherHistoryRepository.save(hold);
        } else {
            VoucherHistory used = new VoucherHistory();
            used.setVoucher(voucher);
            used.setInvoice(invoice);
            used.setCustomer(customer);
            used.setUsedAt(now);
            used.setDiscountValueApplied(invoice.getDiscountAmount());
            used.setStatus(1);
            voucherHistoryRepository.save(used);
        }

        if (voucher.getQuantity() != null) {
            voucher.setQuantity(voucher.getQuantity() - 1);
            voucherRepository.save(voucher);
        }
    }

    @Transactional
    public Invoice removeVoucherFromInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        if (invoice.getVoucher() == null) {
            throw new RuntimeException("Hóa đơn hiện không có voucher để bỏ");
        }

        // Giải phóng giữ chỗ của chính invoice
        releaseOldHolds(invoice);

        invoice.setVoucher(null);
        invoice.setUpdatedDate(new Date());
        invoice.setUpdatedBy(SecurityContextHolder.getContext().getAuthentication().getName());
        updateInvoiceTotal(invoice);

        return invoiceRepository.save(invoice);
    }

    public List<ProductAttributeResponse> getProductAttributesByProductId(Long productId) {
        List<ProductDetail> details = productDetailRepository.findByProductId(productId);
        return invoiceMapper.toProductAttributeResponseList(details);
    }

    public Page<InvoiceResponse> getInvoicesByStatus(int status, int page, int size) {
        TrangThaiTong statusEnum = TrangThaiTong.tuMa(status);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với username: " + username));

        Employee employee = user.getEmployee();
        if (employee == null) {
            throw new RuntimeException("Người dùng không phải là nhân viên.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

        // Lọc thêm orderType = 0
        Page<Invoice> invoicePage =
                invoiceRepository.findByStatusAndEmployeeIdAndOrderType(statusEnum, employee.getId(), 0, pageable);

        return invoicePage.map(invoiceMapper::toInvoiceResponse);
    }


    @Override
    public InvoiceDisplayResponse getInvoiceWithDetails(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hoá đơn"));

        if (invoice.getCustomer() != null) {
            System.out.println("ID khách hàng gắn với hóa đơn: " + invoice.getCustomer().getId());
        } else {
            System.out.println("Hóa đơn không có khách hàng.");
        }

        // Lấy chi tiết: status = 1 và ProductDetail cũng phải status = 1
        List<InvoiceDetail> details =
                invoiceDetailRepository.findByInvoiceAndStatusAndProductDetailActive(invoice, 1);

        List<InvoiceDetailResponse> detailResponses = invoiceMapper.toInvoiceDetailResponseList(details);
        InvoiceResponse invoiceResponse = invoiceMapper.toInvoiceResponse(invoice);

        return new InvoiceDisplayResponse(invoiceResponse, detailResponses);
    }


    @Override
    public Page<InvoiceDisplayResponse> getInvoiceDisplays(Pageable pageable) {
        Page<Invoice> invoices = invoiceRepository.findAll(pageable);

        List<InvoiceDisplayResponse> displayResponses = invoices.stream()
                .map(invoice -> {
                    List<InvoiceDetail> details = invoiceDetailRepository.findByInvoiceId(invoice.getId());
                    return invoiceMapper.toInvoiceDisplayResponse(invoice, details);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(displayResponses, pageable, invoices.getTotalElements());
    }

    @Override
    public Page<InvoiceResponse> searchSeparatedStatus(String keyword,
                                                       String counterStatusKey,
                                                       String onlineStatusKey,
                                                       LocalDate createdDate,
                                                       Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String counterKey = (counterStatusKey != null && !counterStatusKey.isBlank()) ? counterStatusKey.trim() : null;
        String onlineKey  = (onlineStatusKey  != null && !onlineStatusKey.isBlank())  ? onlineStatusKey.trim()  : null;

        Date startOfDay = null, startOfNextDay = null;
        if (createdDate != null) {
            ZonedDateTime zStart = createdDate.atStartOfDay(ZoneId.systemDefault());
            startOfDay = Date.from(zStart.toInstant());
            startOfNextDay = Date.from(zStart.plusDays(1).toInstant());
        }

        Page<Invoice> page = invoiceRepository.searchByKeywordStatusSeparatedAndCreatedDate(
                cleanKeyword, counterKey, onlineKey, startOfDay, startOfNextDay, pageable
        );

        List<InvoiceResponse> dtos = invoiceMapper.toInvoiceResponseList(page.getContent());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    public Invoice findByInvoiceCode(String code) {
        return invoiceRepository.findByInvoiceCode(code);
    }

    @Override
    public List<InvoiceDisplayResponse> getAllInvoicesWithDetails() {
        List<Invoice> invoices = invoiceRepository.findAll();

        return invoices.stream()
                .map(invoice -> {
                    List<InvoiceDetail> details = invoiceDetailRepository.findByInvoiceId(invoice.getId());
                    return invoiceMapper.toInvoiceDisplayResponse(invoice, details);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Invoice findById(Long id) {
        return invoiceRepository.findById(id).orElse(null);
    }

    @Override
    public List<InvoiceDisplayResponse> getInvoicesWithDetailsByIds(List<Long> ids) {
        List<Invoice> invoices = invoiceRepository.findAllById(ids);

        // Lấy toàn bộ chi tiết hóa đơn theo danh sách ID hóa đơn
        List<InvoiceDetail> allDetails = invoiceDetailRepository.findByInvoiceIdIn(ids);

        // Gộp các chi tiết theo invoiceId để ghép đúng hóa đơn
        Map<Long, List<InvoiceDetail>> detailMap = allDetails.stream()
                .collect(Collectors.groupingBy(detail -> detail.getInvoice().getId()));

        // Map từng hóa đơn với danh sách chi tiết tương ứng
        return invoices.stream()
                .map(invoice -> {
                    List<InvoiceDetail> details = detailMap.getOrDefault(invoice.getId(), new ArrayList<>());
                    return invoiceMapper.toInvoiceDisplayResponse(invoice, details);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public InvoiceDisplayResponse createInvoice(InvoiceRequest request) {
        // 1. Xử lý thông tin khách hàng
        Customer customer;
        Long customerId = request.getCustomerInfo().getCustomerId();

        if (customerId != null) {
            customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + customerId));
        } else {
            customer = customerRepository.findTop1ByPhoneAndStatus(request.getCustomerInfo().getPhone(), 1)
                    .orElseGet(() -> {
                        Customer c = new Customer();
                        c.setCustomerName(request.getCustomerInfo().getCustomerName());
                        c.setPhone(request.getCustomerInfo().getPhone());
                        c.setEmail(request.getCustomerInfo().getEmail());
                        c.setStatus(1);
                        c.setCustomerCode("KH" + System.currentTimeMillis());
                        c.setCreatedDate(LocalDateTime.now());
                        return customerRepository.save(c);
                    });
        }

        if (Boolean.TRUE.equals(customer.getIsBlacklisted())) {
            LocalDateTime expiry = customer.getBlacklistExpiryDate();
            if (expiry == null || expiry.isAfter(LocalDateTime.now())) {
                throw new RuntimeException("Khách hàng đang bị cấm mua hàng. Lý do: " + customer.getBlacklistReason());
            }
            customer.setIsBlacklisted(false);
            customer.setBlacklistReason(null);
            customer.setBlacklistExpiryDate(null);
            customerRepository.save(customer);
        }

        AddressRequest addr = request.getCustomerInfo().getAddress();
//        AddressCustomer address = new AddressCustomer();
//        address.setCustomer(customer);
//        address.setCountry(addr.getCountry());
//        address.setProvinceCode(addr.getProvinceCode());
//        address.setProvinceName(addr.getProvinceName());
//        address.setDistrictCode(addr.getDistrictCode());
//        address.setDistrictName(addr.getDistrictName());
//        address.setWardCode(addr.getWardCode());
//        address.setWardName(addr.getWardName());
//        address.setHouseName(addr.getHouseName());
//        address.setStatus(1);
//        address.setCreatedDate(new Date());
//        address.setDefaultAddress(true);
//        addressRepository.save(address);

        String addressInvoice = addr.getHouseName() + " - "
                + addr.getWardName() + " - "
                + addr.getDistrictName() + " - "
                + addr.getProvinceName();

        Invoice invoice = new Invoice();
        invoice.setInvoiceCode("INV" + System.currentTimeMillis());
        invoice.setCustomer(customer);
        invoice.setCreatedDate(new Date());
        invoice.setUpdatedDate(new Date());
        invoice.setDescription(request.getDescription());
        invoice.setOrderType(request.getOrderType());
        invoice.setIsPaid(false);
        invoice.setDeliveryAddress(addressInvoice);
        invoice.setEmail(request.getCustomerInfo().getEmail());
        invoice.setPhone(request.getCustomerInfo().getPhone());
        invoice.setStatus(TrangThaiTong.DANG_XU_LY);
        invoice.setStatusDetail(TrangThaiChiTiet.DANG_GIAO_DICH);
        invoice.setDiscountAmount(Optional.ofNullable(request.getDiscountAmount()).orElse(BigDecimal.ZERO));
        invoice.setShippingFee(Optional.ofNullable(request.getShippingFee()).orElse(BigDecimal.ZERO));

        if (request.getEmployeeId() != null) {
            employeeRepository.findById(request.getEmployeeId()).ifPresent(invoice::setEmployee);
        }

        if (request.getVoucherId() != null) {
            voucherRepository.findById(request.getVoucherId()).ifPresent(invoice::setVoucher);
        }

        // 4. Xử lý sản phẩm
        BigDecimal total = BigDecimal.ZERO;
        List<InvoiceDetail> details = new ArrayList<>();
        Set<ProductDetail> productDetailsToUpdate = new HashSet<>();
        Set<Product> productsToUpdate = new HashSet<>();

        for (CartItemRequest item : request.getItems()) {
            ProductDetail productDetail = productDetailRepository.findById(item.getProductDetailId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm: " + item.getProductDetailId()));

            int currentStock = productDetail.getQuantity();
            if (item.getQuantity() > currentStock) {
                throw new RuntimeException("Số lượng sản phẩm vượt quá tồn kho: " + item.getProductDetailId());
            }
            productDetail.setQuantity(currentStock - item.getQuantity());
            productDetailsToUpdate.add(productDetail);

            Product product = productDetail.getProduct();
            int currentProductStock = product.getQuantity() != null ? product.getQuantity() : 0;
            if (item.getQuantity() > currentProductStock) {
                throw new RuntimeException("Tồn kho tổng không đủ cho sản phẩm: " + product.getId());
            }
            product.setQuantity(currentProductStock - item.getQuantity());
            productsToUpdate.add(product);

            InvoiceDetail detail = new InvoiceDetail();
            detail.setInvoice(invoice);
            detail.setProductDetail(productDetail);
            detail.setQuantity(item.getQuantity());
            detail.setCreatedDate(LocalDateTime.now());
            detail.setStatus(0);
            detail.setInvoiceCodeDetail("INV-DTL-" + UUID.randomUUID().toString().substring(0, 8));

            detail.setSellPrice(item.getSellPrice() != null ? item.getSellPrice() : productDetail.getSellPrice());
            detail.setDiscountedPrice(item.getDiscountedPrice() != null ? item.getDiscountedPrice() : productDetail.getSellPrice());
            detail.setDiscountPercentage(item.getDiscountPercentage() != null ? item.getDiscountPercentage() : 0);

            BigDecimal itemTotal = detail.getDiscountedPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(itemTotal);

            details.add(detail);
        }

        invoice.setTotalAmount(total);
        invoice.setFinalAmount(total
                .subtract(invoice.getDiscountAmount())
                .add(invoice.getShippingFee()));
        invoice.setInvoiceDetails(details);

        // 5. Lưu dữ liệu
        Invoice savedInvoice = invoiceRepository.save(invoice);
        productDetailRepository.saveAll(productDetailsToUpdate);
        productRepository.saveAll(productsToUpdate);

        // 6. Xử lý sau thanh toán
        processInvoicePayment(savedInvoice.getId());

        // ✅ Tự động chặn nếu khách hủy quá nhiều đơn (giống ship code)
        autoBlacklistIfTooManyCancellations(customer);

        // 7. Trả kết quả
        return invoiceMapper.toInvoiceDisplayResponse(savedInvoice, savedInvoice.getInvoiceDetails());
    }

    @Transactional
    @Override
    public InvoiceDisplayResponse createInvoiceShipCode(InvoiceRequest request) {
        try {
            // ==================== 0) Validate cơ bản ====================
            if (request == null || request.getCustomerInfo() == null || request.getItems() == null || request.getItems().isEmpty()) {
                throw new RuntimeException("Thiếu dữ liệu đặt hàng.");
            }
            final LocalDateTime now = LocalDateTime.now();

            // ==================== 1) Xác định/khởi tạo khách hàng ====================
            Customer customer;
            String phoneSender = null;

            Long customerId = request.getCustomerInfo().getCustomerId();
            String phone     = request.getCustomerInfo().getPhone();
            String email     = request.getCustomerInfo().getEmail();
            String name      = request.getCustomerInfo().getCustomerName();

            if (customerId != null) {
                customer = customerRepository.findById(customerId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + customerId));
                phoneSender = customer.getPhone();
            } else {
                if (phone == null || phone.isBlank()) {
                    throw new RuntimeException("Thiếu thông tin khách hàng (cần có customerId hoặc phone).");
                }
                // Tìm theo SĐT
                customer = customerRepository.findTop1ByPhoneAndStatus(phone.trim(), 1).orElse(null);
                if (customer == null) {
                    // Tạo khách vãng lai
                    customer = new Customer();
                    customer.setCustomerName((name == null || name.isBlank()) ? "Khách lẻ" : name.trim());
                    customer.setPhone(phone.trim());
                    customer.setEmail((email == null || email.isBlank()) ? null : email.trim().toLowerCase());
                    customer.setStatus(1);
                    customer.setCustomerCode("KH" + System.currentTimeMillis());
                    customer.setCreatedDate(now);
                    customer.setCreatedBy(currentUsername());
                    customer.setAddressList(new ArrayList<>());
                    customer = customerRepository.save(customer);

                    // === Nếu có email → auto tạo tài khoản (tận dụng các hàm bạn đã có) ===
                    if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
                        final String emailLc = customer.getEmail().trim().toLowerCase();
                        Optional<User> userOpt = userRepository.findByUsername(emailLc);
                        if (userOpt.isEmpty()) {
                            // tạo skeleton user (MK bất kỳ) bằng hàm sẵn có
                            createUserForCustomer(customer, emailLc);

                            // đặt lại MK tạm (6 ký tự như createQuickCustomer) rồi gửi mail
                            Customer finalCustomer = customer;
                            userRepository.findByUsername(emailLc).ifPresent(newUser -> {
                                String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
                                SecureRandom rnd = new SecureRandom();
                                StringBuilder pw = new StringBuilder();
                                for (int i = 0; i < 6; i++) pw.append(chars.charAt(rnd.nextInt(chars.length())));
                                String rawPassword = pw.toString();

                                newUser.setPassword(passwordEncoder.encode(rawPassword));
                                newUser.setUpdatedAt(new Date());
                                newUser.setUpdatedBy(currentUsername());
                                userRepository.save(newUser);

                                // gửi thông tin tài khoản
                                accountEmailService.sendAccountCreatedEmail(
                                        emailLc,
                                        finalCustomer.getCustomerName(),
                                        newUser.getUsername(),
                                        rawPassword
                                );
                            });
                        } else {
                            // user đã tồn tại: đảm bảo gắn customer
                            User u = userOpt.get();
                            if (u.getCustomer() == null) {
                                u.setCustomer(customer);
                                u.setUpdatedAt(new Date());
                                u.setUpdatedBy(currentUsername());
                                userRepository.save(u);
                            }
                        }
                    }
                }
            }

            // ==================== 2) Check blacklist ====================
            if (Boolean.TRUE.equals(customer.getIsBlacklisted())) {
                LocalDateTime expiry = customer.getBlacklistExpiryDate();
                if (expiry == null || expiry.isAfter(now)) {
                    throw new RuntimeException("Khách hàng đang bị cấm mua hàng. Lý do: " + customer.getBlacklistReason());
                }
                // Hết hạn cấm → gỡ cấm
                customer.setIsBlacklisted(false);
                customer.setBlacklistReason(null);
                customer.setBlacklistExpiryDate(null);
                customerRepository.save(customer);
            }

            // ==================== 3) Chuẩn hoá địa chỉ giao hàng ====================
            AddressRequest addr = request.getCustomerInfo().getAddress();
            if (addr == null) throw new RuntimeException("Thiếu địa chỉ giao hàng.");
            String addressNew = addr.getHouseName() + " - " + addr.getWardName() + " - "
                    + addr.getDistrictName() + " - " + addr.getProvinceName() + " - Việt Nam";

            // ==================== 4) Tạo hóa đơn ====================
            Invoice invoice = new Invoice();
            invoice.setInvoiceCode("INV" + System.currentTimeMillis());
            invoice.setCustomer(customer);
            invoice.setCreatedDate(new Date());
            invoice.setUpdatedDate(new Date());
            invoice.setPhone(phone);
            if(phoneSender != null){
                invoice.setPhoneSender(phoneSender);
            }else{
                invoice.setPhoneSender(phone);
            }
            invoice.setDescription(request.getDescription());
            invoice.setOrderType(request.getOrderType()); // Online
            invoice.setIsPaid(false);
            invoice.setStatus(TrangThaiTong.DANG_XU_LY);
            invoice.setStatusDetail(TrangThaiChiTiet.CHO_XU_LY);
            invoice.setDiscountAmount(Optional.ofNullable(request.getDiscountAmount()).orElse(BigDecimal.ZERO));
            invoice.setShippingFee(Optional.ofNullable(request.getShippingFee()).orElse(BigDecimal.ZERO));
            invoice.setDeliveryAddress(addressNew);

            if (request.getEmployeeId() != null) {
                employeeRepository.findById(request.getEmployeeId()).ifPresent(invoice::setEmployee);
            }
            if (request.getVoucherCode() != null) {
                voucherRepository.findByVoucherCode(request.getVoucherCode()).ifPresent(invoice::setVoucher);
            }

            // ==================== 5) Áp khuyến mãi & kiểm tồn ====================
            final BigDecimal ONE_HUNDRED = new BigDecimal("100");
            List<DiscountCampaign> activeCampaigns = discountCampaignRepository.findActiveCampaigns(now);

            BigDecimal total = BigDecimal.ZERO;
            List<InvoiceDetail> details = new ArrayList<>();
            Map<Long, Integer> productDetailQty = new HashMap<>();

            for (CartItemRequest item : request.getItems()) {
                // 5.1 Lấy PD & kiểm tồn thực (trừ số lượng giữ chỗ/đơn đang mở)
                ProductDetail pd = productDetailRepository.findByIdAndStatus(item.getProductDetailId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm chi tiết ID: " + item.getProductDetailId()));
                Product product = pd.getProduct();

                int qty = Optional.ofNullable(item.getQuantity()).orElse(0);
                if (qty <= 0) throw new RuntimeException("Số lượng không hợp lệ cho SPCT: " + pd.getId());

                Integer reserved = Optional.ofNullable(reservationOrderRepository.sumQuantityByProductDetailActive(pd.getId())).orElse(0);
                int available = pd.getQuantity() - reserved;
                if (qty > available) {
                    throw new RuntimeException("Số lượng vượt quá tồn kho: " + pd.getId());
                }
                productDetailQty.put(pd.getId(), qty);

                // 5.2 Giá gốc từ DB
                BigDecimal sellPrice = Optional.ofNullable(pd.getSellPrice())
                        .orElse(Optional.ofNullable(product.getSellPrice()).orElse(BigDecimal.ZERO));

                // 5.3 Tìm % giảm tốt nhất: ƯU TIÊN SPCT -> Product -> Campaign
                // PD-level (ưu tiên % riêng ở link; nếu null dùng % campaign)
                Optional<AbstractMap.SimpleEntry<Long, BigDecimal>> pdBest = activeCampaigns.stream()
                        .filter(c -> c.getProductDetails() != null)
                        .flatMap(c -> c.getProductDetails().stream()
                                .filter(link -> link.getProductDetail() != null && link.getProductDetail().getId().equals(pd.getId()))
                                .map(link -> new AbstractMap.SimpleEntry<>(
                                        c.getId(),
                                        link.getDiscountPercentage() != null ? link.getDiscountPercentage()
                                                : Optional.ofNullable(c.getDiscountPercentage()).orElse(BigDecimal.ZERO)
                                )))
                        .max(Comparator.comparing(e -> e.getValue(), Comparator.nullsFirst(BigDecimal::compareTo)));

                // Product-level (nếu có % riêng ở link product thì lấy, không thì dùng % campaign)
                Optional<AbstractMap.SimpleEntry<Long, BigDecimal>> pBest = activeCampaigns.stream()
                        .filter(c -> c.getProducts() != null)
                        .flatMap(c -> c.getProducts().stream()
                                .filter(link -> link.getProduct() != null && link.getProduct().getId().equals(product.getId()))
                                .map(link -> new AbstractMap.SimpleEntry<>(
                                        c.getId(),
                                        // Nếu bạn có cột link.getDiscountPercentage() thì thay thế bên dưới cho đúng
                                        Optional.ofNullable(c.getDiscountPercentage()).orElse(BigDecimal.ZERO)
                                )))
                        .max(Comparator.comparing(e -> e.getValue(), Comparator.nullsFirst(BigDecimal::compareTo)));

                Long bestCampaignId = null;
                BigDecimal bestPercent = BigDecimal.ZERO;

                if (pdBest.isPresent()) {
                    bestCampaignId = pdBest.get().getKey();
                    bestPercent = pdBest.get().getValue();
                } else if (pBest.isPresent()) {
                    bestCampaignId = pBest.get().getKey();
                    bestPercent = pBest.get().getValue();
                }

                // 5.4 Nếu FE gửi discountCampaignId và hợp lệ -> ép dùng campaign đó (ưu tiên % ở SPCT nếu có)
                if (item.getDiscountCampaignId() != null) {
                    Optional<DiscountCampaign> chosenOpt = activeCampaigns.stream()
                            .filter(c -> c.getId().equals(item.getDiscountCampaignId()))
                            .findFirst();
                    if (chosenOpt.isPresent()) {
                        DiscountCampaign chosen = chosenOpt.get();
                        boolean applicable =
                                (chosen.getProductDetails() != null && chosen.getProductDetails().stream()
                                        .anyMatch(link -> link.getProductDetail() != null && link.getProductDetail().getId().equals(pd.getId())))
                                        ||
                                        (chosen.getProducts() != null && chosen.getProducts().stream()
                                                .anyMatch(link -> link.getProduct() != null && link.getProduct().getId().equals(product.getId())));
                        if (applicable) {
                            Optional<BigDecimal> chosenPdPct = (chosen.getProductDetails() == null) ? Optional.empty()
                                    : chosen.getProductDetails().stream()
                                    .filter(link -> link.getProductDetail() != null && link.getProductDetail().getId().equals(pd.getId()))
                                    .map(DiscountCampaignProductDetail::getDiscountPercentage)
                                    .filter(Objects::nonNull)
                                    .findFirst();
                            bestCampaignId = chosen.getId();
                            bestPercent = chosenPdPct.orElse(Optional.ofNullable(chosen.getDiscountPercentage()).orElse(BigDecimal.ZERO));
                        }
                    }
                }
                if (bestPercent == null) bestPercent = BigDecimal.ZERO;

                // 5.5 Tính giá sau giảm
                BigDecimal discountedPrice = (bestPercent.compareTo(BigDecimal.ZERO) > 0)
                        ? sellPrice.multiply(ONE_HUNDRED.subtract(bestPercent)).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP)
                        : sellPrice.setScale(2, RoundingMode.HALF_UP);

                // 5.6 Tạo dòng hóa đơn
                InvoiceDetail detail = new InvoiceDetail();
                detail.setInvoice(invoice);
                detail.setProductDetail(pd);
                detail.setQuantity(qty);
                detail.setCreatedDate(now);
                detail.setStatus(1);
                detail.setInvoiceCodeDetail("INV-DTL-" + UUID.randomUUID().toString().substring(0, 8));
                detail.setSellPrice(sellPrice);
                detail.setDiscountedPrice(discountedPrice);
                detail.setDiscountPercentage(bestPercent.intValue());

                if (bestCampaignId != null) {
                    discountCampaignRepository.findById(bestCampaignId).ifPresent(detail::setDiscountCampaign);
                }

                details.add(detail);
                total = total.add(discountedPrice.multiply(BigDecimal.valueOf(qty)));
            }

            // ==================== 6) Tổng tiền và lưu hóa đơn ====================
            invoice.setTotalAmount(total);
            invoice.setFinalAmount(
                    total
                            .subtract(Optional.ofNullable(invoice.getDiscountAmount()).orElse(BigDecimal.ZERO))
                            .add(Optional.ofNullable(invoice.getShippingFee()).orElse(BigDecimal.ZERO))
            );
            invoice.setInvoiceDetails(details);

            Invoice savedInvoice = invoiceRepository.save(invoice);

            // ==================== 7) Giữ hàng (Reservation) ====================
            List<ReservationOrder> reservations = new ArrayList<>();
            productDetailQty.forEach((pdId, qty) -> {
                ReservationOrder ro = new ReservationOrder();
                ro.setProductDetailId(pdId);
                ro.setInvoiceId(savedInvoice.getId());
                ro.setQuantity(qty);
                ro.setStatus(1);
                ro.setCreatedAt(new Date());
                reservations.add(ro);
            });
            reservationOrderRepository.saveAll(reservations);

            // ==================== 8) Voucher (nếu có) ====================
            markVoucherUsedIfAny(savedInvoice, now);

            // ==================== 9) Tạo giao dịch COD ====================
            InvoiceTransaction transaction = new InvoiceTransaction();
            transaction.setTransactionCode("GD-" + UUID.randomUUID().toString().substring(0, 8));
            transaction.setInvoice(savedInvoice);
            transaction.setAmount(savedInvoice.getFinalAmount());
            transaction.setPaymentStatus(1);
            transaction.setPaymentMethod("Thanh toán khi nhận hàng");
            transaction.setTransactionType("Thanh toán sau");
            transaction.setNote(null);
            transaction.setPaymentTime(new Date());
            invoiceTransactionRepository.save(transaction);

            // ==================== 10) Hậu xử lý ====================
            processInvoicePayment(savedInvoice.getId());           // cập nhật trạng thái liên quan thanh toán
            autoBlacklistIfTooManyCancellations(customer);         // nếu bạn có rule auto blacklist

            // ==================== 11) Gửi email hóa đơn ====================
            if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
                invoiceEmailService.sendInvoiceEmail(savedInvoice);
            }

            // ==================== 12) Trả về DTO ====================
            return invoiceMapper.toInvoiceDisplayResponse(savedInvoice, savedInvoice.getInvoiceDetails());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Lỗi khi tạo đơn hàng: " + ex.getMessage());
        }
    }


    @Override
    public void autoBlacklistIfTooManyCancellations(Customer customer) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastCheckedLdt = Optional.ofNullable(customer.getLastBlacklistChecked())
                .orElse(now.minusDays(30));

        Date lastCheckedDate = Date.from(lastCheckedLdt.atZone(ZoneId.systemDefault()).toInstant());

        int newCancelledOrders = invoiceRepository.countByCustomerAndStatusDetailAndUpdatedDateAfter(
                customer,
                TrangThaiChiTiet.HUY_DON,
                lastCheckedDate
        );

        int newSuccessfulOrders = invoiceRepository.countByCustomerAndStatusAndUpdatedDateAfter(
                customer,
                TrangThaiTong.THANH_CONG, // đổi lại nếu enum trạng thái hoàn tất của bạn khác tên
                lastCheckedDate
        );

        // ===== Luật ân xá =====
        if (newSuccessfulOrders > 0) {
            resetBlacklistState(customer, now);
            customerRepository.save(customer);
            return;
        }
        // ======================

        if (newCancelledOrders <= 0) {
            customer.setLastBlacklistChecked(now);
            customerRepository.save(customer);
            return;
        }

        int previousCancelCount = Optional.ofNullable(customer.getLastBlacklistCancelCount()).orElse(0);
        int totalCancelled = previousCancelCount + newCancelledOrders;

        int currentScore = Optional.ofNullable(customer.getTrustScore()).orElse(100);
        int deducted = newCancelledOrders * 10;
        int newTrustScore = Math.max(0, currentScore - deducted);
        customer.setTrustScore(newTrustScore);

        // ⚠️ Cảnh báo nếu hủy 3 hoặc 4 đơn (chỉ log khi lần đầu chạm mốc)
        if ((totalCancelled == 3 || totalCancelled == 4) && previousCancelCount < totalCancelled) {
            String warningMsg = "⚠️ Cảnh báo: Đã hủy " + totalCancelled + " đơn hàng. "
                    + "Nếu hủy đến 5 đơn sẽ bị cấm mua hàng 3 ngày.";

            CustomerBlacklistHistory warning = new CustomerBlacklistHistory();
            warning.setCustomer(customer);
            warning.setReason(warningMsg);
            warning.setStartTime(now);
            warning.setEndTime(null); // cảnh báo, không có hạn
            customerBlacklistHistoryRepository.save(warning);

            customer.setBlacklistReason(warningMsg);
        }

        //  Cấm nếu hủy từ 5 đơn trở lên (vừa mới chạm ngưỡng)
        if (totalCancelled >= 5 && previousCancelCount < 5) {
            customer.setIsBlacklisted(true);
            customer.setBlacklistReason("Đã hủy ≥ 5 đơn hàng");
            customer.setBlacklistExpiryDate(now.plusDays(3));

            CustomerBlacklistHistory blacklist = new CustomerBlacklistHistory();
            blacklist.setCustomer(customer);
            blacklist.setReason(customer.getBlacklistReason());
            blacklist.setStartTime(now);
            blacklist.setEndTime(customer.getBlacklistExpiryDate());
            customerBlacklistHistoryRepository.save(blacklist);
        }

        // Cập nhật cuối
        customer.setLastBlacklistChecked(now);
        customer.setLastBlacklistCancelCount(totalCancelled);
        customerRepository.save(customer);
    }

    @Override
    public void resetBlacklistState(Customer customer, LocalDateTime now) {
        List<CustomerBlacklistHistory> openHistories =
                customerBlacklistHistoryRepository.findOpenHistoriesByCustomer(customer.getId());
        for (CustomerBlacklistHistory h : openHistories) {
            h.setEndTime(now);
        }
        if (!openHistories.isEmpty()) {
            customerBlacklistHistoryRepository.saveAll(openHistories);
        }

        customer.setIsBlacklisted(false);
        customer.setBlacklistReason(null);
        customer.setBlacklistExpiryDate(null);
        customer.setLastBlacklistCancelCount(0);

        // Khôi phục điểm tin cậy (tuỳ chính sách: set 100 hoặc +10,…)
        customer.setTrustScore(100);

        customer.setLastBlacklistChecked(now);

        // Ghi nhận 1 bản ghi ân xá
        CustomerBlacklistHistory pardon = new CustomerBlacklistHistory();
        pardon.setCustomer(customer);
        pardon.setReason("Ân xá: Có đơn giao thành công sau lần kiểm tra trước, reset trạng thái.");
        pardon.setStartTime(now);
        pardon.setEndTime(now);
        customerBlacklistHistoryRepository.save(pardon);
    }

    @Transactional
    @Override
    public InvoiceWithZaloPayResponse createInvoiceAndZaloPay(InvoiceRequest request) throws Exception {
        // ===== 0) Validate =====
        if (request == null || request.getCustomerInfo() == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Thiếu dữ liệu đơn hàng hoặc danh sách sản phẩm.");
        }

        // ===== 1) KHÁCH HÀNG =====
        Customer customer;
        Long customerId = request.getCustomerInfo().getCustomerId();
        String phone = request.getCustomerInfo().getPhone();

        String phoneSender = null;
        if (customerId != null) {
            customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + customerId));
            phoneSender = customer.getPhone();
        } else if (phone != null && !phone.isBlank()) {
            customer = customerRepository.findTop1ByPhoneAndStatus(phone, 1).orElse(null);
            if (customer == null) {
                customer = new Customer();
                customer.setCustomerName(request.getCustomerInfo().getCustomerName());
                customer.setPhone(phone);
                customer.setEmail(request.getCustomerInfo().getEmail());
                customer.setStatus(1);
                customer.setCustomerCode("KH" + System.currentTimeMillis());
                customer.setCreatedDate(LocalDateTime.now());
                customer.setAddressList(new ArrayList<>());
                customer = customerRepository.save(customer);
            }
        } else {
            throw new RuntimeException("Thiếu thông tin khách hàng (cần có customerId hoặc phone).");
        }

        // Blacklist check
        if (Boolean.TRUE.equals(customer.getIsBlacklisted())) {
            LocalDateTime expiry = customer.getBlacklistExpiryDate();
            if (expiry == null || expiry.isAfter(LocalDateTime.now())) {
                throw new RuntimeException("Khách hàng đang bị cấm mua hàng. Lý do: " + customer.getBlacklistReason());
            }
            // Nếu blacklist đã hết hạn, bỏ flag
            customer.setIsBlacklisted(false);
            customer.setBlacklistReason(null);
            customer.setBlacklistExpiryDate(null);
            customerRepository.save(customer);
        }

        // ===== 2) ĐỊA CHỈ GIAO HÀNG =====
        AddressRequest addr = request.getCustomerInfo().getAddress();
        String addressNew = (addr == null) ? null
                : String.join(" - ",
                Optional.ofNullable(addr.getHouseName()).orElse(""),
                Optional.ofNullable(addr.getWardName()).orElse(""),
                Optional.ofNullable(addr.getDistrictName()).orElse(""),
                Optional.ofNullable(addr.getProvinceName()).orElse(""),
                "Việt Nam"
        ).replaceAll("( - )+", " - ").replaceAll("^\\s*-\\s*", "").replaceAll("\\s*-\\s*$", "");

        // ===== 3) HÓA ĐƠN (chưa lưu) =====
        Invoice invoice = new Invoice();
        invoice.setInvoiceCode("INV" + System.currentTimeMillis());
        invoice.setCustomer(customer);
        invoice.setCreatedDate(new Date());
        invoice.setUpdatedDate(new Date());
        invoice.setDescription(request.getDescription());
        invoice.setOrderType(request.getOrderType()); // Ví dụ: ONLINE
        invoice.setIsPaid(false);
        invoice.setStatus(TrangThaiTong.DANG_XU_LY);
        invoice.setStatusDetail(TrangThaiChiTiet.DANG_GIAO_DICH); // Chờ khách thanh toán
        invoice.setDiscountAmount(Optional.ofNullable(request.getDiscountAmount()).orElse(BigDecimal.ZERO));
        invoice.setShippingFee(Optional.ofNullable(request.getShippingFee()).orElse(BigDecimal.ZERO));
        invoice.setDeliveryAddress(addressNew);
        invoice.setEmail(request.getCustomerInfo().getEmail());
        invoice.setPhone(request.getCustomerInfo().getPhone());
        invoice.setPhoneSender(phoneSender);

        if (request.getEmployeeId() != null) {
            employeeRepository.findById(request.getEmployeeId()).ifPresent(invoice::setEmployee);
        }

        // ==== Voucher: CHỈ set khi voucher tìm thấy (không tạo new Voucher() tránh TransientObjectException) ====
        if (request.getVoucherId() != null) {
            voucherRepository.findById(request.getVoucherId()).ifPresent(invoice::setVoucher);
        } else if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            // nếu frontend gửi voucherCode thay vì id -> tìm theo code và set nếu có
            voucherRepository.findByVoucherCode(request.getVoucherCode())
                    .ifPresent(invoice::setVoucher);
        }
        // Lưu ý: nếu muốn validate voucher (status, quantity, time window, minOrderValue...) -> kiểm tra ở đây và throw nếu không hợp lệ.

        // ===== 4) TÍNH TOÁN & TẠO HDCT (chỉ kiểm tra tồn kho, KHÔNG trừ kho) =====
        final BigDecimal ONE_HUNDRED = new BigDecimal("100");
        List<DiscountCampaign> activeCampaigns = discountCampaignRepository.findActiveCampaigns(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        List<InvoiceDetail> details = new ArrayList<>();

        Map<Long,Integer> productDetailQty = new HashMap<>();
        for (CartItemRequest item : request.getItems()) {
            // Lấy ProductDetail
            ProductDetail pd = productDetailRepository.findById(item.getProductDetailId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm chi tiết ID: " + item.getProductDetailId()));

            productDetailQty.put(item.getProductDetailId(), item.getQuantity());
            Integer quantityReserOrder = reservationOrderRepository.sumQuantityByProductDetailActive(pd.getId());

            // Kiểm tra tồn kho (chi tiết)
            Integer quantity = pd.getQuantity() - quantityReserOrder;
            if (item.getQuantity() > quantity) {
                throw new RuntimeException("Số lượng vượt quá tồn kho chi tiết sản phẩm: " + item.getProductDetailId());
            }

            Product product = pd.getProduct();

            // Giá gốc lấy từ pd (nếu null dùng product)
            BigDecimal sellPrice = Optional.ofNullable(pd.getSellPrice())
                    .orElse(Optional.ofNullable(product.getSellPrice()).orElse(BigDecimal.ZERO));

            // --- Tìm % giảm tốt nhất từ các campaign đang hoạt động ---
            Long bestCampaignId = null;
            BigDecimal bestPercent = BigDecimal.ZERO;

            // 1) ưu tiên link ProductDetail trong campaign
            Optional<AbstractMap.SimpleEntry<Long, BigDecimal>> pdBest = activeCampaigns.stream()
                    .filter(c -> c.getProductDetails() != null)
                    .flatMap(c -> c.getProductDetails().stream()
                            .filter(link -> link.getProductDetail() != null && link.getProductDetail().getId().equals(pd.getId()))
                            .map(link -> new AbstractMap.SimpleEntry<>(
                                    c.getId(),
                                    link.getDiscountPercentage() != null ? link.getDiscountPercentage()
                                            : Optional.ofNullable(c.getDiscountPercentage()).orElse(BigDecimal.ZERO)
                            )))
                    .max(Comparator.comparing(e -> e.getValue(), Comparator.nullsFirst(BigDecimal::compareTo)));

            // 2) fallback: % theo Product
            Optional<AbstractMap.SimpleEntry<Long, BigDecimal>> pBest = activeCampaigns.stream()
                    .filter(c -> c.getProducts() != null)
                    .flatMap(c -> c.getProducts().stream()
                            .filter(link -> link.getProduct() != null && link.getProduct().getId().equals(product.getId()))
                            .map(link -> new AbstractMap.SimpleEntry<>(
                                    c.getId(),
                                    Optional.ofNullable(c.getDiscountPercentage()).orElse(BigDecimal.ZERO)
                            )))
                    .max(Comparator.comparing(e -> e.getValue(), Comparator.nullsFirst(BigDecimal::compareTo)));

            if (pdBest.isPresent()) {
                bestCampaignId = pdBest.get().getKey();
                bestPercent = pdBest.get().getValue();
            } else if (pBest.isPresent()) {
                bestCampaignId = pBest.get().getKey();
                bestPercent = pBest.get().getValue();
            }

            // 3) Nếu FE ép campaign cụ thể, và campaign đó áp dụng cho PD/Product -> dùng campaign đó
            if (item.getDiscountCampaignId() != null) {
                Optional<DiscountCampaign> chosenOpt = activeCampaigns.stream()
                        .filter(c -> c.getId().equals(item.getDiscountCampaignId()))
                        .findFirst();
                if (chosenOpt.isPresent()) {
                    DiscountCampaign chosen = chosenOpt.get();
                    boolean applicable =
                            (chosen.getProductDetails() != null && chosen.getProductDetails().stream()
                                    .anyMatch(link -> link.getProductDetail() != null && link.getProductDetail().getId().equals(pd.getId())))
                                    ||
                                    (chosen.getProducts() != null && chosen.getProducts().stream()
                                            .anyMatch(link -> link.getProduct() != null && link.getProduct().getId().equals(product.getId())));
                    if (applicable) {
                        Optional<BigDecimal> chosenPdPct = (chosen.getProductDetails() == null) ? Optional.empty()
                                : chosen.getProductDetails().stream()
                                .filter(link -> link.getProductDetail() != null && link.getProductDetail().getId().equals(pd.getId()))
                                .map(link -> link.getDiscountPercentage())
                                .filter(Objects::nonNull)
                                .findFirst();
                        BigDecimal chosenPct = chosenPdPct.orElse(
                                Optional.ofNullable(chosen.getDiscountPercentage()).orElse(BigDecimal.ZERO)
                        );
                        bestCampaignId = chosen.getId();
                        bestPercent = chosenPct;
                    }
                }
            }

            if (bestPercent == null) bestPercent = BigDecimal.ZERO;

            BigDecimal discountedPrice = (bestPercent.compareTo(BigDecimal.ZERO) > 0)
                    ? sellPrice.multiply(ONE_HUNDRED.subtract(bestPercent)).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP)
                    : sellPrice.setScale(2, RoundingMode.HALF_UP);

            InvoiceDetail detail = new InvoiceDetail();
            detail.setInvoice(invoice); // invoice chưa save nhưng sẽ được cascade nếu mapping đúng
            detail.setProductDetail(pd);
            detail.setQuantity(item.getQuantity());
            detail.setCreatedDate(LocalDateTime.now());
            detail.setStatus(1);
            detail.setInvoiceCodeDetail("INV-DTL-" + UUID.randomUUID().toString().substring(0, 8));

            detail.setSellPrice(sellPrice);
            detail.setDiscountedPrice(discountedPrice);
            // Lưu phần trăm làm int (làm tròn nửa trên)
            detail.setDiscountPercentage(bestPercent.setScale(0, RoundingMode.HALF_UP).intValue());

            if (bestCampaignId != null) {
                discountCampaignRepository.findById(bestCampaignId).ifPresent(detail::setDiscountCampaign);
            }

            BigDecimal lineTotal = discountedPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(lineTotal);
            details.add(detail);
        }

        invoice.setTotalAmount(total);
        invoice.setFinalAmount(
                total
                        .subtract(Optional.ofNullable(invoice.getDiscountAmount()).orElse(BigDecimal.ZERO))
                        .add(Optional.ofNullable(invoice.getShippingFee()).orElse(BigDecimal.ZERO))
        );
        // Gán danh sách details vào invoice (chi tiết đã set invoice reference)
        invoice.setInvoiceDetails(details);

        // Lưu invoice (với cascade invoiceDetails nếu mapping đúng)
        Invoice saved = invoiceRepository.save(invoice);
//        final Long invoiceId = saved.getId();
//
//        List<ReservationOrder> orders = new ArrayList<>();
//        productDetailQty.forEach((pdId, qty) -> {
//            ReservationOrder order = new ReservationOrder();
//            order.setInvoiceId(invoiceId);
//            order.setProductDetailId(pdId);
//            order.setQuantity(qty);
//            order.setStatus(1);
//            order.setCreatedAt(new  Date());
//            orders.add(order);
//        });
//        reservationOrderRepository.saveAll(orders);

        // Tạo appTransId dựa trên id saved
        String appTransId = new SimpleDateFormat("yyMMdd").format(new Date()) + "_" + saved.getId();
        ZaloPayResponse zaloPayResponse = zaloPayService.createZaloPayOrder(
                saved.getPhone(),
                saved.getFinalAmount(),
                "Thanh toán đơn hàng #" + saved.getInvoiceCode(),
                appTransId
        );

        // Lưu lại appTransId vào HĐ
        saved.setAppTransId(appTransId);
        saved.setUpdatedDate(new Date());
        saved = invoiceRepository.save(saved);

        InvoiceDisplayResponse display = invoiceMapper.toInvoiceDisplayResponse(saved, saved.getInvoiceDetails());
        return new InvoiceWithZaloPayResponse(display, zaloPayResponse);
    }

    @Transactional
    @Override
    public Invoice getInvoice(String appTransId) {
        if (appTransId == null || appTransId.trim().isEmpty()) {
            throw new IllegalArgumentException("appTransId không được phép null/empty");
        }

        Invoice invoice = invoiceRepository.findByAppTransId(appTransId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn với appTransId: " + appTransId));

        // Idempotency: CHỈ trừ kho khi chi tiết đã ở trạng thái ĐÃ_XỬ_LÝ
        if (invoice.getStatusDetail() != TrangThaiChiTiet.CHO_XU_LY) {
            // Không phải thời điểm trừ kho -> trả nguyên trạng
            return invoice;
        }

        List<InvoiceDetail> details = invoice.getInvoiceDetails();
        if (details == null || details.isEmpty()) {
            // Không có detail -> hủy/hard-fail theo nghiệp vụ
            invoice.setStatus(TrangThaiTong.HUY_GIAO_DICH);
            invoice.setStatusDetail(TrangThaiChiTiet.HUY_GIAO_DICH);
            invoice.setUpdatedDate(new Date());
            invoiceRepository.save(invoice);
            throw new RuntimeException("Invoice không có chi tiết để trừ kho (appTransId=" + appTransId + ")");
        }

        List<ProductDetail> pdToSave = new ArrayList<>();
        List<Product> pToSave = new ArrayList<>();

        Map<Long,Integer> productDetailQty = new HashMap<>();
        // Duyệt và lock từng ProductDetail / Product trước khi trừ
        for (InvoiceDetail detail : details) {
            if (detail.getProductDetail() == null || detail.getProductDetail().getId() == null) {
                invoice.setStatus(TrangThaiTong.HUY_GIAO_DICH);
                invoice.setStatusDetail(TrangThaiChiTiet.HUY_GIAO_DICH);
                invoice.setUpdatedDate(new Date());
                invoiceRepository.save(invoice);
                throw new RuntimeException("InvoiceDetail thiếu productDetail (invoiceId=" + invoice.getId() + ")");
            }

            Long pdId = detail.getProductDetail().getId();
            Integer soLuong = detail.getQuantity();
            productDetailQty.put(pdId, soLuong);

            // Lock row ProductDetail (SELECT ... FOR UPDATE)
            ProductDetail pd = productDetailRepository.findByIdForUpdate(pdId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ProductDetail khi trừ kho: " + pdId));

            Integer quantityKho = Optional.ofNullable(pd.getQuantity()).orElse(0);
            Integer quantityReser = reservationOrderRepository.sumQuantityByProductDetailActive(pdId);

            int availPd = quantityKho - quantityReser;
            int need = Optional.ofNullable(detail.getQuantity()).orElse(0);

            if (need > availPd) {
                invoice.setStatus(TrangThaiTong.HUY_GIAO_DICH);
                invoice.setStatusDetail(TrangThaiChiTiet.HUY_GIAO_DICH);
                invoice.setUpdatedDate(new Date());
                invoiceRepository.save(invoice);
                throw new RuntimeException("Tồn kho chi tiết không đủ cho pdId=" + pdId + " (cần " + need + ", còn " + availPd + ")");
            }
            pdToSave.add(pd);

            Long productId = (pd.getProduct() != null) ? pd.getProduct().getId() : null;
            if (productId == null) {
                invoice.setStatus(TrangThaiTong.HUY_GIAO_DICH);
                invoice.setStatusDetail(TrangThaiChiTiet.HUY_GIAO_DICH);
                invoice.setUpdatedDate(new Date());
                invoiceRepository.save(invoice);
                throw new RuntimeException("Product liên kết với ProductDetail không hợp lệ (pdId=" + pdId + ")");
            }

            Product p = productRepository.findByIdProduct(productId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Product id=" + productId));

            pToSave.add(p);
        }

        // Commit thay đổi kho
        productDetailRepository.saveAll(pdToSave);
        productRepository.saveAll(pToSave);

        // Cập nhật invoice: đánh dấu đã trừ kho (và đã thanh toán nếu nghiệp vụ yêu cầu)
        invoice.setIsPaid(true); // hoặc giữ nguyên nếu bạn set ở bước thanh toán
        // Giữ nguyên trạng thái chi tiết là ĐÃ_XỬ_LÝ (không set về CHO_XU_LY nữa)
        invoice.setUpdatedDate(new Date());

        Invoice saved = invoiceRepository.save(invoice);
                final Long invoiceId = saved.getId();

        List<ReservationOrder> orders = new ArrayList<>();
        productDetailQty.forEach((pdId, qty) -> {
            ReservationOrder order = new ReservationOrder();
            order.setInvoiceId(invoiceId);
            order.setProductDetailId(pdId);
            order.setQuantity(qty);
            order.setStatus(1);
            order.setCreatedAt(new  Date());
            orders.add(order);
        });
        reservationOrderRepository.saveAll(orders);

        // Các bước hậu xử lý (ví dụ: mark voucher used)
        LocalDateTime now = LocalDateTime.now();
        markVoucherUsedIfAny(saved, now);

        return saved;
    }



    @Transactional
    public void handleZaloPayCallback(String appTransId) {

    }

    @Transactional
    public void updateInvoiceStatusByAppTransId(String appTransId, Integer status,Integer statusDetail,Boolean isPaid) {
        TrangThaiTong statusEnum = TrangThaiTong.tuMa(status);
        TrangThaiChiTiet statusDetailEnum = TrangThaiChiTiet.tuMa(statusDetail);
        invoiceRepository.findByAppTransId(appTransId).ifPresent(invoice -> {
            invoice.setIsPaid(isPaid);
            invoice.setStatus(statusEnum);
            invoice.setStatusDetail(statusDetailEnum);
            invoice.setUpdatedDate(new Date());

            InvoiceTransaction invoiceTransaction = new InvoiceTransaction();
            invoiceTransaction.setTransactionCode("GD-" + UUID.randomUUID().toString().substring(0, 8));
            invoiceTransaction.setInvoice(invoice);
            invoiceTransaction.setAmount(invoice.getFinalAmount());
            invoiceTransaction.setPaymentStatus(1);
            invoiceTransaction.setPaymentMethod("Transfer");
            invoiceTransaction.setTransactionType("Payment in advance");
            invoiceTransaction.setNote(null);
            invoiceTransaction.setPaymentTime(new Date());
            invoiceTransactionRepository.save(invoiceTransaction);
            invoiceRepository.save(invoice);
        });
    }

    @Override
    public void updateStatusIfPaid(String appTransId) throws Exception {
        JSONObject response = zaloPayService.queryOrder(appTransId);

        int returnCode = response.optInt("returncode");
        int bcTransStatus = response.optInt("bctransstatus");

        if (returnCode == 1 && bcTransStatus == 1) {
            updateInvoiceStatusByAppTransId(appTransId, 0,0,true); // PAID
            log.info(" Đơn {} đã được cập nhật thành PAID từ kết quả query ZaloPay", appTransId);
        }else if (returnCode != 1) {
            updateInvoiceStatusByAppTransId(appTransId, -1,-1,false); // FAIL
        }  else {
            log.warn(" Đơn {} chưa thanh toán (status={})", appTransId, bcTransStatus);
        }
    }

    private static final int MONEY_SCALE = 0; // VND
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    private BigDecimal money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(MONEY_SCALE, RM);
    }
    private int clampPercent(Integer p) {
        if (p == null) return 0;
        return Math.max(0, Math.min(100, p));
    }
    private String safeInvoiceCode() { return "INV-" + System.currentTimeMillis(); }
    private String currentUsername() {
        try { return SecurityContextHolder.getContext().getAuthentication().getName(); }
        catch (Exception e) { return "SYSTEM"; }
    }

    @Transactional
    @Override
    public InvoiceWithVnpayResponse createInvoiceAndVnpay(InvoiceRequest request) throws Exception {
        // Bước 1: Tạo hóa đơn
        InvoiceDisplayResponse invoiceDisplay = this.createInvoice(request);
        Long invoiceId = invoiceDisplay.getInvoice().getId();

        // Bước 2: Tạo orderId
        String orderId = new SimpleDateFormat("yyMMdd").format(new Date()) + "_" + invoiceId;

        // Bước 3: Gọi VNPay
        VnpayResponse vnpayResponse = vnpayService.createVnpayOrder(
                orderId,
                invoiceDisplay.getInvoice().getFinalAmount(),
                "Thanh toán đơn hàng #" + invoiceDisplay.getInvoice().getInvoiceCode()
        );

        // Bước 4: Lưu orderId vào DB
        invoiceRepository.findById(invoiceId).ifPresent(invoice -> {
            invoice.setAppTransId(orderId); // dùng lại trường appTransId
            invoice.setUpdatedDate(new Date());
            invoiceRepository.save(invoice);
        });

        return new InvoiceWithVnpayResponse(invoiceDisplay, vnpayResponse);
    }

    @Override
    public List<InvoiceResponse> searchInvoices(InvoiceSearchRequest request) {
        TrangThaiChiTiet statusEnum  = TrangThaiChiTiet.tuMa(request.getStatusDetail());

        TrangThaiChiTiet statusChoXuLy = TrangThaiChiTiet.CHO_XU_LY;
        TrangThaiChiTiet statusDaXacNhan = TrangThaiChiTiet.DA_XU_LY;
        TrangThaiChiTiet statusChoGiaoHang = TrangThaiChiTiet.CHO_GIAO_HANG;
        TrangThaiChiTiet statusDangGiao = TrangThaiChiTiet.DANG_GIAO_HANG;
        TrangThaiChiTiet statusGiaoThanhCong = TrangThaiChiTiet.GIAO_THANH_CONG;
        TrangThaiChiTiet statusGiaoThatBai = TrangThaiChiTiet.GIAO_THAT_BAI;
        TrangThaiChiTiet statusHuy = TrangThaiChiTiet.HUY_DON;

        List<InvoiceResponse> invoices = invoiceRepository.searchInvoices(statusEnum,request.getIsPaid(),1,request.getCreatedFrom(),request.getCreatedTo(),request.getPhone(),request.getCode(),statusChoXuLy,statusDaXacNhan,
                statusChoGiaoHang,statusDangGiao,statusGiaoThanhCong,statusGiaoThatBai,statusHuy);
        return invoices;
    }

    public PurchaseStats getCustomerPurchaseStats(Long customerId) {
        List<Invoice> invoices = invoiceRepository.findSuccessfulOnlineInvoicesByCustomerId(customerId, TrangThaiTong.THANH_CONG);

        BigDecimal total = invoices.stream()
                .map(Invoice::getFinalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int count = invoices.size();

        PurchaseStats stats = new PurchaseStats();
        stats.setInvoiceCount(count);
        stats.setTotalSpent(total);
        return stats;
    }

    public CustomerTier classifyCustomer(PurchaseStats stats) {
        BigDecimal total = stats.getTotalSpent();
        int count = stats.getInvoiceCount();

        if (count >= 20 && total.compareTo(BigDecimal.valueOf(20_000_000)) >= 0) {
            return CustomerTier.VIP;
        } else if (count >= 10 && total.compareTo(BigDecimal.valueOf(10_000_000)) >= 0) {
            return CustomerTier.THAN_THIET;
        } else if (count >= 3 && total.compareTo(BigDecimal.valueOf(3_000_000)) >= 0) {
            return CustomerTier.THANH_VIEN_THUONG;
        } else {
            return CustomerTier.MOI;
        }
    }

    @Override
    public PromotionSuggestion getSuggestedPromotion(Long customerId) {
        PurchaseStats stats = getCustomerPurchaseStats(customerId);
        CustomerTier tier = classifyCustomer(stats);

        PromotionSuggestion suggestion = new PromotionSuggestion();
        suggestion.setTier(tier);
        suggestion.setInvoiceCount(stats.getInvoiceCount());

        switch (tier) {
            case VIP -> {
                suggestion.setMessage("Chúc mừng! Bạn là khách VIP. Ưu đãi 100K dành riêng cho bạn!");
                suggestion.setCouponCode("VIP20");
                suggestion.setDiscountAmount(BigDecimal.valueOf(100_000));
            }
            case THAN_THIET -> {
                suggestion.setMessage("Cảm ơn bạn đã đồng hành! Nhận ưu đãi 50K.");
                suggestion.setCouponCode("LOYAL10");
                suggestion.setDiscountAmount(BigDecimal.valueOf(50_000));
            }
            case THANH_VIEN_THUONG -> {
                suggestion.setMessage("Tiếp tục mua sắm để lên hạng cao hơn! Nhận ưu đãi 30K.");
                suggestion.setCouponCode("REGULAR5");
                suggestion.setDiscountAmount(BigDecimal.valueOf(30_000));
            }
            case MOI -> {
                suggestion.setMessage("Ưu đãi chào mừng! Giảm ngay 20K cho đơn hàng tiếp theo.");
                suggestion.setCouponCode("WELCOME");
                suggestion.setDiscountAmount(BigDecimal.valueOf(20_000));
            }
        }

        return suggestion;
    }

    @Transactional
    @Override
    public void processInvoicePayment(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        invoice.setStatus(TrangThaiTong.DANG_XU_LY);
        LocalDateTime now = LocalDateTime.now();
        Date updatedDate = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        invoice.setUpdatedDate(updatedDate);

        invoiceRepository.save(invoice);

        // Gửi tặng voucher nếu đủ điều kiện
        checkAndGiftVoucher(invoice.getCustomer().getId());
    }

    @Override
    public void checkAndGiftVoucher(Long customerId) {
        PurchaseStats stats = getCustomerPurchaseStats(customerId);
        CustomerTier tier = classifyCustomer(stats);

        switch (tier) {
            case VIP -> {
                if (!hasVoucher(customerId, 100000)) {
                    createVoucher(customerId, "VIP20", "Chúc mừng! Bạn là khách VIP. Ưu đãi 100K dành riêng cho bạn!", 100000);
                }
            }
            case THAN_THIET -> {
                if (!hasVoucher(customerId, 50000)) {
                    createVoucher(customerId, "LOYAL10", "Cảm ơn bạn đã đồng hành! Nhận ưu đãi 50K.", 50000);
                }
            }
            case THANH_VIEN_THUONG -> {
                if (!hasVoucher(customerId, 30000)) {
                    createVoucher(customerId, "REGULAR5", "Tiếp tục mua sắm để lên hạng cao hơn! Nhận ưu đãi 30K.", 30000);
                }
            }
            case MOI -> {
                if (!hasVoucher(customerId, 20000)) {
                    createVoucher(customerId, "WELCOME", "Ưu đãi chào mừng! Giảm ngay 20K cho đơn hàng tiếp theo.", 20000);
                }
            }
        }
    }

    @Override
    public TrangThaiChiTiet findStatusByCode(String code) {
        return invoiceRepository.findStatusDetailByCode(code);
    }

    @Override
    public List<Integer> findDiscountCampianByCode(String code) {
        List<Integer> status = invoiceRepository.getDiscountCampaignStatus(code);
        return status;
    }

    private boolean hasVoucher(Long customerId, int amount) {
        return voucherRepository.existsByCustomerIdAndDiscountAmount(customerId, amount);
    }

    private void createVoucher(Long customerId, String code, String name, int discountAmount) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        Voucher voucher = new Voucher();
        voucher.setCustomer(customer);
        voucher.setVoucherCode(code + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase()); // eg: VIP20-4F7A1B
        voucher.setVoucherName(name);
        voucher.setDiscountAmount(discountAmount);
        voucher.setMinOrderValue(BigDecimal.ZERO);
        voucher.setStartDate(LocalDateTime.now());
        voucher.setEndDate(LocalDateTime.now().plusDays(30));
        voucher.setCreatedDate(LocalDateTime.now());
        voucher.setStatus(1); // đang hoạt động

        voucherRepository.save(voucher);
    }

    public Long getSoldQuantityByProduct(Long productId) {
        return invoiceDetailRepository.countSoldQuantityByProductId(productId);
    }
}