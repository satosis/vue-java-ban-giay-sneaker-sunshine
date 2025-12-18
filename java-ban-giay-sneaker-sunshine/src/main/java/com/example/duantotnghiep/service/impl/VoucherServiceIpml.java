package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.dto.request.VoucherRequest;
import com.example.duantotnghiep.dto.request.VoucherSearchRequest;
import com.example.duantotnghiep.dto.response.PaginationDTO;
import com.example.duantotnghiep.dto.response.VoucherResponse;
import com.example.duantotnghiep.dto.response.VoucherStatusDTO;
import com.example.duantotnghiep.mapper.VoucherMapper;
import com.example.duantotnghiep.model.Invoice;
import com.example.duantotnghiep.model.InvoiceDetail;
import com.example.duantotnghiep.model.Product;
import com.example.duantotnghiep.model.ProductCategory;
import com.example.duantotnghiep.model.Voucher;
import com.example.duantotnghiep.repository.*;
import com.example.duantotnghiep.service.VoucherService;
import com.example.duantotnghiep.state.TrangThaiTong;
import com.example.duantotnghiep.xuatExcel.VoucherExportExcel;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherServiceIpml implements VoucherService {
    private final VoucherRepository voucherRepository;
    private final VoucherHistoryRepository voucherHistoryRepository;
    private final VoucherMapper voucherMapper;
    private final InvoiceRepository invoiceRepository;
    private final VoucherSearchRepository voucherSearchRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final VoucherNativeRepository voucherNativeRepository;
    private final InvoiceServiceImpl invoiceServiceImpl;

    public List<VoucherResponse> getValidVouchers() {
        LocalDateTime now = LocalDateTime.now();
        List<Voucher> vouchers = voucherRepository.findValidVouchers(now);

        return vouchers.stream()
                .map(voucherMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<VoucherResponse> getVouchersByCustomerInInvoice(Long invoiceId) {
        // 1) Lấy hóa đơn + kiểm tra KH
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn với ID: " + invoiceId));

        if (invoice.getCustomer() == null) {
            throw new RuntimeException("Hóa đơn không có khách hàng.");
        }

        Integer orderType = invoice.getOrderType(); // 0: quầy, 1: online
        LocalDateTime now = LocalDateTime.now();
        Long customerId = invoice.getCustomer().getId();

        // 2) Thu thập productId & categoryId từ các dòng HỢP LỆ (nếu có)
        Set<Long> productIds = new HashSet<>();
        Set<Long> categoryIds = new HashSet<>();

        List<InvoiceDetail> details = invoice.getInvoiceDetails();
        if (details != null) {
            for (InvoiceDetail d : details) {
                if (d == null) continue;
                // status null hoặc =1 và quantity > 0
                boolean okStatus = (d.getStatus() == null || d.getStatus() == 1);
                boolean okQty    = (d.getQuantity() != null && d.getQuantity() > 0);
                if (!okStatus || !okQty) continue;

                if (d.getProductDetail() != null && d.getProductDetail().getProduct() != null) {
                    Product p = d.getProductDetail().getProduct();
                    if (p.getId() != null) {
                        productIds.add(p.getId());

                        // lấy category của product
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
            }
        }

        // 3) Xác định cờ lọc
        boolean hasProductIds   = !productIds.isEmpty();
        boolean hasCategoryIds  = !categoryIds.isEmpty();
        boolean useProducts     = hasProductIds;         // có productIds thì mới lọc theo sản phẩm
        boolean useCategories   = hasCategoryIds;        // có categoryIds thì mới lọc theo danh mục

        // 4) Gọi repo: nếu không có ids, truyền Set rỗng + cờ false để query không ép điều kiện IN
        List<Voucher> vouchers = voucherRepository.findValidVouchers(
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

        // 5) Lọc voucher đã dùng + quantity <= 0 (an toàn)
        Set<Long> usedVoucherIds = Optional.ofNullable(
                        voucherHistoryRepository.findByCustomerIdAndStatus(customerId, 1)
                ).orElseGet(Collections::emptyList).stream()
                .filter(Objects::nonNull)
                .map(vh -> vh.getVoucher() == null ? null : vh.getVoucher().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Voucher> availableVouchers = vouchers.stream()
                .filter(v -> v.getId() != null && !usedVoucherIds.contains(v.getId()))
                .filter(v -> v.getQuantity() == null || v.getQuantity() > 0)
                .toList();

        // 6) Map DTO
        return availableVouchers.stream()
                .map(voucherMapper::toDto)
                .toList();
    }

    private void validateRequest(VoucherRequest r, boolean isCreate) {
        // ===== 1) Bắt buộc 1 trong 2: % giảm hoặc tiền giảm (và chỉ 1) =====
        boolean hasPct = r.getDiscountPercentage() != null && r.getDiscountPercentage().compareTo(BigDecimal.ZERO) > 0;
        boolean hasAmt = r.getDiscountAmount() != null && r.getDiscountAmount() > 0;
        if (hasPct == hasAmt) { // cả 2 true hoặc cả 2 false
            throw new IllegalArgumentException("Phải nhập đúng 1 trong 2: phần trăm giảm hoặc số tiền giảm.");
        }

        // ===== 2) Ràng buộc giá trị số =====
        if (r.getDiscountPercentage() != null) {
            if (r.getDiscountPercentage().compareTo(BigDecimal.ZERO) <= 0
                    || r.getDiscountPercentage().compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Phần trăm giảm phải trong (0; 100].");
            }
        }
        if (r.getDiscountAmount() != null && r.getDiscountAmount() <= 0) {
            throw new IllegalArgumentException("Số tiền giảm phải > 0.");
        }

        if (r.getMinOrderValue() == null || r.getMinOrderValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá trị đơn tối thiểu phải ≥ 0.");
        }
        if (r.getMaxDiscountValue() != null && r.getMaxDiscountValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giảm tối đa phải ≥ 0.");
        }
        if (r.getMinOrderToReceive() != null && r.getMinOrderToReceive().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá trị đơn tối thiểu để NHẬN voucher phải ≥ 0.");
        }

        // (Tùy chọn) Nếu dùng % giảm thì nên có giảm tối đa để bảo vệ doanh thu
        // if (hasPct && r.getMaxDiscountValue() == null) {
        //     throw new IllegalArgumentException("Vui lòng nhập 'Giảm tối đa' khi dùng phần trăm giảm.");
        // }

        // ===== 3) Thời gian áp dụng =====
        if (r.getStartDate() == null || r.getEndDate() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu/kết thúc không được để trống.");
        }
        if (!r.getEndDate().isAfter(r.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu.");
        }
        if (isCreate && r.getStartDate().isBefore(LocalDateTime.now())) {
            // Nếu không muốn ràng buộc này thì bỏ khối if
            throw new IllegalArgumentException("Ngày bắt đầu phải ≥ hiện tại khi tạo mới.");
        }

        // ===== 4) Order/Voucher type =====
        if (r.getOrderType() == null || (r.getOrderType() != 0 && r.getOrderType() != 1)) {
            throw new IllegalArgumentException("Loại đơn hàng không hợp lệ (0: tại quầy, 1: online).");
        }
        if (r.getVoucherType() == null || (r.getVoucherType() != 1 && r.getVoucherType() != 2)) {
            throw new IllegalArgumentException("Loại voucher không hợp lệ (1: công khai, 2: riêng tư).");
        }
        if (r.getVoucherType() == 2 && r.getCustomerId() == null) {
            throw new IllegalArgumentException("Voucher riêng tư phải chọn khách hàng.");
        }

        // ===== 5) Phạm vi áp dụng (chỉ chọn 1 trong 2) =====
        if (r.getProductId() != null && r.getCategoryId() != null) {
            throw new IllegalArgumentException("Chỉ được chọn 1 trong 2: sản phẩm hoặc danh mục.");
        }

        // ===== 6) Số lượng =====
        if (r.getQuantity() == null || r.getQuantity() < 1) {
            throw new IllegalArgumentException("Số lượng phải ≥ 1.");
        }
    }


    @Override
    public VoucherResponse themMoi(VoucherRequest voucherRequest) {
        validateRequest(voucherRequest, true); // <<<<<<<<<<

        Voucher voucher = voucherMapper.toEntity(voucherRequest);

        if (voucherRequest.getCustomerId() != null) {
            voucher.setCustomer(customerRepository.findById(voucherRequest.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng")));
        }
        if (voucherRequest.getEmployeeId() != null) {
            voucher.setEmployee(employeeRepository.findById(voucherRequest.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên")));
        }
        if (voucherRequest.getProductId() != null) {
            voucher.setProduct(productRepository.findById(voucherRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm")));
        }
        if (voucherRequest.getCategoryId() != null) {
            voucher.setCategory(categoryRepository.findById(voucherRequest.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục")));
        }

        voucher.setMinOrderToReceive(voucherRequest.getMinOrderToReceive());
        voucher.setVoucherCode(generateVoucherCode());
        voucher.setCreatedDate(LocalDateTime.now());
        voucher.setCreatedBy("admin");

        return voucherMapper.toDto(voucherRepository.save(voucher));
    }

    @Override
    public VoucherResponse capNhat(Long id, VoucherRequest voucherRequest) {
        validateRequest(voucherRequest, false); // <<<<<<<<<<

        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

        voucherMapper.updateVoucherFromDto(voucherRequest, voucher);

        if (voucherRequest.getCustomerId() != null) {
            voucher.setCustomer(customerRepository.findById(voucherRequest.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng")));
        } else {
            voucher.setCustomer(null);
        }
        if (voucherRequest.getEmployeeId() != null) {
            voucher.setEmployee(employeeRepository.findById(voucherRequest.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên")));
        } else {
            voucher.setEmployee(null);
        }
        if (voucherRequest.getProductId() != null) {
            voucher.setProduct(productRepository.findById(voucherRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm")));
        } else {
            voucher.setProduct(null);
        }
        if (voucherRequest.getCategoryId() != null) {
            voucher.setCategory(categoryRepository.findById(voucherRequest.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục")));
        } else {
            voucher.setCategory(null);
        }

        voucher.setMinOrderToReceive(voucherRequest.getMinOrderToReceive());
        voucher.setUpdatedDate(LocalDateTime.now());
        voucher.setUpdatedBy("admin");

        return voucherMapper.toDto(voucherRepository.save(voucher));
    }


    @Override
    public Optional<VoucherResponse> getOne(Long id) {
        return voucherRepository.findById(id)
                .map(voucherMapper::toDto);
    }

    @Override
    @Transactional
    public void deteleVoucherById(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy voucher với ID: " + id));

        // Nếu đã tắt rồi thì báo luôn
        if (Objects.equals(voucher.getStatus(), 0)) {
            throw new IllegalStateException("Voucher này đã bị xoá trước đó.");
        }

        voucher.setStatus(0); // soft-delete
        voucher.setUpdatedBy("system");
        voucher.setUpdatedDate(LocalDateTime.now());
        voucherRepository.save(voucher);

        // Tìm các hóa đơn đang xử lý và chưa thanh toán còn tham chiếu voucher này
        List<Invoice> affected = invoiceRepository
                .findAllByVoucherIdAndStatusAndUnpaid(id, TrangThaiTong.DANG_XU_LY);

        for (Invoice inv : affected) {
            inv.setVoucher(null);                 // gỡ voucher
            inv.setUpdatedBy("system");
            inv.setUpdatedDate(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
            invoiceRepository.save(inv);

            // Tính lại tổng tiền (đã không còn giảm theo voucher)
            invoiceServiceImpl.updateInvoiceTotal(inv);
        }
    }

    @Override
    public PaginationDTO<VoucherResponse> phanTrangHienThi(VoucherSearchRequest request, Pageable pageable) {
        return voucherSearchRepository.searchVouchers(request,pageable);
    }

    private String generateVoucherCode() {
        String prefix = "VOUCHER-";
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + datePart + "-" + randomPart;
    }

    @Override
    public Voucher validateVoucher(Long customerId, String voucherCode, BigDecimal orderTotal) {
        Voucher voucher = voucherRepository.findByCustomerIdOrGlobalVoucherCode(customerId, voucherCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher hợp lệ"));

        LocalDateTime now = LocalDateTime.now();

        if ((voucher.getStartDate() != null && voucher.getStartDate().isAfter(now)) ||
                (voucher.getEndDate() != null && voucher.getEndDate().isBefore(now))) {
            throw new RuntimeException("Voucher đã hết hạn hoặc chưa đến thời gian sử dụng");
        }

        if (voucher.getQuantity() != null && voucher.getQuantity() <= 0) {
            throw new RuntimeException("Voucher đã hết lượt sử dụng");
        }

        if (voucher.getMinOrderValue() != null && orderTotal.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng voucher");
        }

        if (voucher.getStatus() == null || voucher.getStatus() != 1) {
            throw new RuntimeException("Voucher đang không hoạt động");
        }

        return voucher;
    }

    @Override
    public Voucher validateVoucherV2(Long customerId, String voucherCode, BigDecimal orderTotal) {
        Voucher voucher = voucherRepository.findAvailableByCustomerIdAndCode(customerId, voucherCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher hợp lệ"));

        LocalDateTime now = LocalDateTime.now();

        if ((voucher.getStartDate() != null && voucher.getStartDate().isAfter(now)) ||
                (voucher.getEndDate() != null && voucher.getEndDate().isBefore(now))) {
            throw new RuntimeException("Voucher đã hết hạn hoặc chưa đến thời gian sử dụng");
        }

        if (voucher.getQuantity() != null && voucher.getQuantity() <= 0) {
            throw new RuntimeException("Voucher đã hết lượt sử dụng");
        }

        if (voucher.getMinOrderValue() != null && orderTotal.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng voucher");
        }

        if (voucher.getStatus() == null || voucher.getStatus() != 1) {
            throw new RuntimeException("Voucher đang không hoạt động");
        }

        return voucher;
    }


    @Transactional(readOnly = true)
    public Voucher findBestVoucherForCustomer(Long invoiceId, BigDecimal orderTotal) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn: " + invoiceId));

        if (invoice.getCustomer() == null) {
            throw new RuntimeException("Hóa đơn không có khách hàng.");
        }

        // Lọc dòng hợp lệ
        List<InvoiceDetail> validDetails = Optional.ofNullable(invoice.getInvoiceDetails())
                .orElse(List.of()).stream()
                .filter(d -> d != null
                        && (d.getStatus() == null || d.getStatus() == 1)
                        && d.getQuantity() != null && d.getQuantity() > 0
                        && d.getProductDetail() != null
                        && d.getProductDetail().getProduct() != null
                        && d.getProductDetail().getProduct().getId() != null)
                .toList();

        if (validDetails.isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống, không thể tự động áp voucher.");
        }

        // Gom productIds (đúng kiểu Long)
        Set<Long> productIds = validDetails.stream()
                .map(d -> d.getProductDetail().getProduct().getId())
                .collect(Collectors.toSet());

        if (productIds.isEmpty()) {
            throw new RuntimeException("Không có sản phẩm hợp lệ trong giỏ để áp voucher theo sản phẩm.");
        }

        Long customerId = invoice.getCustomer().getId();
        Integer orderType = invoice.getOrderType(); // 0: quầy, 1: online
        LocalDateTime now = LocalDateTime.now();

        // ===== GỌI QUERY STRICT THEO SẢN PHẨM =====
        List<Voucher> candidates = voucherRepository.findValidVouchersStrictByProducts(
                now, orderType, productIds
        );

        // Hàng rào cuối: chỉ giữ voucher có product trùng với productIds
        List<Voucher> applicable = candidates.stream()
                .filter(v -> v.getProduct() != null
                        && v.getProduct().getId() != null
                        && productIds.contains(v.getProduct().getId()))
                .toList();

        if (applicable.isEmpty()) {
            throw new RuntimeException("Không có voucher nào khớp đúng sản phẩm trong giỏ.");
        }

        // Voucher đã dùng
        Set<Long> usedVoucherIds = voucherHistoryRepository
                .findByCustomerIdAndStatus(customerId, 1)
                .stream()
                .map(vh -> vh.getVoucher() != null ? vh.getVoucher().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Chọn voucher tốt nhất
        Voucher best = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;

        for (Voucher v : applicable) {
            if (usedVoucherIds.contains(v.getId())) continue;
            if (v.getMinOrderValue() != null && orderTotal.compareTo(v.getMinOrderValue()) < 0) continue;

            BigDecimal discount = BigDecimal.ZERO;

            // % giảm
            if (v.getDiscountPercentage() != null && v.getDiscountPercentage().compareTo(BigDecimal.ZERO) > 0) {
                discount = orderTotal.multiply(v.getDiscountPercentage())
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                if (v.getMaxDiscountValue() != null && discount.compareTo(v.getMaxDiscountValue()) > 0) {
                    discount = v.getMaxDiscountValue();
                }
            }
            // Số tiền cố định
            else if (v.getDiscountAmount() != null
                    && BigDecimal.valueOf(v.getDiscountAmount().longValue()).compareTo(BigDecimal.ZERO) > 0) {
                discount = BigDecimal.valueOf(v.getDiscountAmount().longValue());
            }

            if (discount.compareTo(bestDiscount) > 0) {
                bestDiscount = discount;
                best = v;
            }
        }

        if (best == null) {
            throw new RuntimeException("Không có voucher nào đạt điều kiện (đơn chưa đủ min hoặc voucher đã dùng).");
        }

        return best;
    }

    @Transactional(readOnly = true)
    @Override
    public List<VoucherResponse> getVouchersByCustomer(
            Long customerId,
            Integer orderType,
            Set<Long> productIds,
            Set<Long> categoryIds
    ) {
        if (customerId == null) {
            return Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now();

        boolean hasProductIds  = productIds != null && !productIds.isEmpty();
        boolean hasCategoryIds = categoryIds != null && !categoryIds.isEmpty();

        // Quyết định dùng product hay category (giữ logic của bạn: ưu tiên product khi có)
        int useProducts   = hasProductIds ? 1 : 0;
        int useCategories = (!hasProductIds && hasCategoryIds) ? 1 : 0;

        int hasP = hasProductIds ? 1 : 0;
        int hasC = hasCategoryIds ? 1 : 0;

        // Nếu rỗng, truyền placeholder tránh IN (null). Vì hasP/hasC = 0, clause OR :hasProductIds = 0 sẽ true.
        Set<Long> safeProductIds  = hasProductIds ? productIds : Collections.singleton(-1L);
        Set<Long> safeCategoryIds = hasCategoryIds ? categoryIds : Collections.singleton(-1L);

        // Convert now -> Timestamp for SQL Server
        LocalDateTime nowTs = LocalDateTime.now();

        List<Voucher> vouchers = voucherRepository.findValidVouchersV2Native(
                nowTs,
                customerId,
                orderType,
                useProducts,
                useCategories,
                hasP,
                hasC,
                safeProductIds,
                safeCategoryIds
        );

        // Lấy voucher đã dùng (status = 1)
        Set<Long> usedVoucherIds = Optional.ofNullable(
                        voucherHistoryRepository.findByCustomerIdAndStatus(customerId, 1)
                ).orElseGet(Collections::emptyList).stream()
                .filter(Objects::nonNull)
                .map(vh -> vh.getVoucher() == null ? null : vh.getVoucher().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Voucher> available = vouchers.stream()
                .filter(v -> v != null && v.getId() != null && !usedVoucherIds.contains(v.getId()))
                .filter(v -> v.getQuantity() == null || v.getQuantity() > 0)
                .collect(Collectors.toList());

        return available.stream()
                .map(voucherMapper::toDto)
                .collect(Collectors.toList());
    }


    @Override
    public void exportVoucherToExcelByIds(List<Long> voucherIds, OutputStream outputStream) throws IOException {
        List<Voucher> vouchers = voucherRepository.getVoucherByIds(voucherIds);
        try (ByteArrayInputStream excelStream = VoucherExportExcel.exportProductToExcel(vouchers)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = excelStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush(); // đảm bảo đẩy hết dữ liệu
        } catch (IOException e) {
            throw new IOException("Không thể xuất Excel theo ID: " + e.getMessage(), e);
        }
    }

    @Override
    public VoucherStatusDTO getVoucherStatsForToday(Long voucherId) {
        ZoneId zone = ZoneId.systemDefault();

        LocalDate today = LocalDate.now(zone);
        ZonedDateTime startZdt = today.atStartOfDay(zone);
        ZonedDateTime endZdt = startZdt.plusDays(1);

        Date start = Date.from(startZdt.toInstant());
        Date end = Date.from(endZdt.toInstant());

        return voucherNativeRepository.findStatusByVoucherId(voucherId, start, end);
    }

    @Override
    public Integer getStatus(String code) {
        Integer s = voucherRepository.getVoucherStatus(code);
        return s;
    }


}
