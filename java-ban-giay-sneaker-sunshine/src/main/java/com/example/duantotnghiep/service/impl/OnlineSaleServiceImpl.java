package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.dto.request.OrderRequest;
import com.example.duantotnghiep.dto.request.UpdateAddress;
import com.example.duantotnghiep.dto.response.*;
import com.example.duantotnghiep.model.*;
import com.example.duantotnghiep.repository.*;
import com.example.duantotnghiep.service.CustomerService;
import com.example.duantotnghiep.service.InvoiceService;
import com.example.duantotnghiep.service.NotificationService;
import com.example.duantotnghiep.service.OnlineSaleService;
import com.example.duantotnghiep.service.VoucherEmailService;
import com.example.duantotnghiep.state.TrangThaiChiTiet;
import com.example.duantotnghiep.state.TrangThaiTong;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnlineSaleServiceImpl implements OnlineSaleService {
    private final InvoiceRepository invoiceRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final InvoiceTransactionRepository invoiceTransactionRepository;
    private final InvoiceDetailRepository invoiceDetailRepository;
    private final InvoiceRepository2 invoiceRepository2;
    private final ProductDetailRepository  productDetailRepository;
    private final ProductRepository productRepository;
    private final InvoiceService invoiceService;
    private final VoucherRepository voucherRepository;
    private final VoucherEmailService voucherEmailService;
    private final InvoiceEmailService invoiceEmailService;
    private final InvoiceEmailServiceStatus invoiceEmailServiceStatus;
    private final NotificationService notificationService;
    private final ReservationOrderRepository reservationOrderRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    @Override
    public void chuyenTrangThai(Long invoiceId, String nextKey) {
        final Date now = new Date();

        // 1) Lấy user & employee hiện tại
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Không xác định được người dùng hiện tại.");
        }
        final String username = auth.getName();

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + username));

        Employee employee = currentUser.getEmployee();
        if (employee == null || employee.getId() == null) {
            throw new RuntimeException("Người dùng không phải nhân viên hoặc thiếu ID nhân viên.");
        }

        // 2) Lấy invoice
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // 3) Check đang được xử lý bởi NV khác
        Boolean lockedByAnother = historyRepository.isProcessedByAnotherEmployee(invoiceId, employee.getId());
        if (Boolean.TRUE.equals(lockedByAnother)) {
            throw new RuntimeException("Đơn hàng đang được xử lý bởi nhân viên khác.");
        }

        // 4) Resolve & validate trạng thái kế tiếp
        final TrangThaiChiTiet nextStatus = parseStatusKey(nextKey);
        final TrangThaiChiTiet currentStatus = invoice.getStatusDetail();

        if (currentStatus == nextStatus) {
            throw new RuntimeException("Trạng thái mới trùng với trạng thái hiện tại.");
        }
        if (!canTransit(currentStatus, nextStatus)) {
            throw new RuntimeException("Không thể chuyển từ " +
                    (currentStatus == null ? "NULL" : currentStatus.name()) +
                    " sang " + nextStatus.name());
        }

        if (currentStatus == TrangThaiChiTiet.CHO_XU_LY) {
            // Lấy chi tiết hóa đơn với trạng thái phù hợp
            List<InvoiceDetail> details = invoiceDetailRepository.findByInvoiceIdAndStatus(invoiceId);

            for (InvoiceDetail detail : details) {
                ProductDetail productDetail = detail.getProductDetail();

                // Lấy số lượng đặt trong hóa đơn
                int orderQuantity = detail.getQuantity();

                // Lấy số lượng tồn hiện tại
                int currentStock = productDetail.getQuantity();

                // Trừ số lượng tồn
                int newStock = currentStock - orderQuantity;

                if (newStock < 0) {
                    throw new IllegalArgumentException("Sản phẩm " + productDetail.getId() + " không đủ số lượng trong kho");
                }

                Integer update = reservationOrderRepository.deactivateByProductDetailAndInvoice(productDetail.getId(),invoiceId);
                if(update <= 0 || update == null){
                    throw new IllegalStateException("Chuyển trạng thái thất bại");
                }
                productDetail.setQuantity(newStock);

                // Lưu lại thay đổi
                productDetailRepository.save(productDetail);
            }
        }


        // 5) Gán NV phụ trách
        invoice.setEmployee(employee);

        // 6) Cập nhật theo trạng thái chi tiết
        invoice.setStatusDetail(nextStatus);
        if (nextStatus == TrangThaiChiTiet.GIAO_THANH_CONG && invoice.getDeliveredAt() == null) {
            invoice.setDeliveredAt(now);
        }

        // 7) Map trạng thái tổng
        invoice.setStatus(mapTongFromDetail(nextStatus, invoice.getStatus()));

        // 8) Lưu hoá đơn
        Invoice saved = invoiceRepository.save(invoice);

        if(saved.getCustomer() != null && saved.getStatusDetail() == TrangThaiChiTiet.GIAO_THANH_CONG){
            LocalDateTime time = LocalDateTime.now();
            Long customerId =  saved.getCustomer().getId();
            Customer customer = customerRepository.findCustomerByIdAndStatus(customerId,1).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            invoiceService.resetBlacklistState(customer,time);
        }

        // 9) Ghi lịch sử
        OrderStatusHistory history = new OrderStatusHistory();
        history.setInvoice(saved);
        history.setEmployee(employee);
        history.setOldStatus(currentStatus != null ? currentStatus.getMa() : null);
        history.setNewStatus(nextStatus.getMa());
        history.setChangedAt(now);
        historyRepository.save(history);

        // 10) Gửi email (không rollback nếu lỗi)
        if (nextStatus == TrangThaiChiTiet.DANG_GIAO_HANG
                || nextStatus == TrangThaiChiTiet.GIAO_THANH_CONG
                || nextStatus == TrangThaiChiTiet.GIAO_THAT_BAI
                || nextStatus == TrangThaiChiTiet.DA_HOAN_TIEN) {
            try {
                invoiceEmailServiceStatus.sendStatusEmail(saved, nextStatus);
            } catch (Exception mailEx) {
                log.warn("Email trạng thái thất bại cho đơn {}: {}", saved.getInvoiceCode(), mailEx.getMessage());
            }
        }

        // 11) Auto tặng voucher (không rollback nếu lỗi)
        try {
            handleAutoPromoVoucher(saved, username, LocalDateTime.now());
        } catch (Exception ex) {
            log.warn("Auto promo voucher lỗi cho đơn {}: {}", saved.getInvoiceCode(), ex.getMessage());
        }

        // 12) Bắn thông báo realtime (không rollback nếu lỗi)
        try {
            Long customerId = (saved.getCustomer() != null) ? saved.getCustomer().getId() : null;

            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "ORDER_STATUS");
            payload.put("title", "Đơn " + saved.getInvoiceCode() + " cập nhật");
            payload.put("content", "Trạng thái: " + labelOf(nextStatus));
            payload.put("invoiceCode", saved.getInvoiceCode());
            payload.put("nextStatus", nextStatus.name());
            payload.put("at", Instant.now().toString());
            payload.put("customerId", customerId);
            payload.put("employeeId", employee.getId());

            if (customerId != null) {
                // An toàn: đẩy vào user-queue cho mọi account thuộc KH
                notificationService.sendToAllUsersOfCustomer(customerId, payload);

                // Tuỳ chọn: topic theo KH (đã guard SUBSCRIBE nên không lộ)
                notificationService.sendToCustomerId(customerId, payload);

            }

            //  BỎ hẳn broadcast để không lộ thông tin
            // notificationService.sendBroadcast(payload);

        } catch (Exception notifyEx) {
            log.warn("Notify realtime thất bại cho đơn {}: {}", saved.getInvoiceCode(), notifyEx.getMessage());
        }

    }

    /* ================== Helpers ================== */

    // Luồng chuyển hợp lệ — chỉnh theo nghiệp vụ
    private static final Map<TrangThaiChiTiet, Set<TrangThaiChiTiet>> ALLOWED_TRANSITIONS =
            Map.ofEntries(
                    Map.entry(TrangThaiChiTiet.DANG_GIAO_DICH, Set.of(TrangThaiChiTiet.CHO_XU_LY, TrangThaiChiTiet.HUY_GIAO_DICH)),

                    Map.entry(TrangThaiChiTiet.CHO_XU_LY, Set.of(TrangThaiChiTiet.DA_XU_LY, TrangThaiChiTiet.HUY_DON)),
                    Map.entry(TrangThaiChiTiet.DA_XU_LY, Set.of(TrangThaiChiTiet.CHO_GIAO_HANG, TrangThaiChiTiet.HUY_DON)),

                    Map.entry(TrangThaiChiTiet.CHO_GIAO_HANG, Set.of(TrangThaiChiTiet.DANG_GIAO_HANG, TrangThaiChiTiet.HUY_DON)),
                    Map.entry(TrangThaiChiTiet.DANG_GIAO_HANG, Set.of(TrangThaiChiTiet.GIAO_THANH_CONG, TrangThaiChiTiet.GIAO_THAT_BAI, TrangThaiChiTiet.MAT_HANG, TrangThaiChiTiet.HUY_DON)),

                    Map.entry(TrangThaiChiTiet.GIAO_THAT_BAI, Set.of(TrangThaiChiTiet.CHO_GIAO_HANG, TrangThaiChiTiet.YEU_CAU_HOAN, TrangThaiChiTiet.DA_HOAN_TIEN)),
                    Map.entry(TrangThaiChiTiet.MAT_HANG, Set.of(TrangThaiChiTiet.DA_HOAN_TIEN)),

                    Map.entry(TrangThaiChiTiet.YEU_CAU_HOAN, Set.of(TrangThaiChiTiet.DA_HOAN_TIEN)),
                    Map.entry(TrangThaiChiTiet.DA_HOAN_TIEN, Set.of(TrangThaiChiTiet.DA_HOAN_THANH)),

                    Map.entry(TrangThaiChiTiet.GIAO_THANH_CONG, Set.of(TrangThaiChiTiet.DA_HOAN_THANH)),

                    Map.entry(TrangThaiChiTiet.HUY_DON, Set.of()),
                    Map.entry(TrangThaiChiTiet.HUY_GIAO_DICH, Set.of()),
                    Map.entry(TrangThaiChiTiet.DA_HOAN_THANH, Set.of())
            );

    private TrangThaiChiTiet parseStatusKey(String key) {
        if (key == null) throw new IllegalArgumentException("Thiếu khóa trạng thái.");
        final String k = key.trim();

        for (TrangThaiChiTiet v : TrangThaiChiTiet.values()) {
            if (v.name().equalsIgnoreCase(k)) return v;
        }
        try {
            for (TrangThaiChiTiet v : TrangThaiChiTiet.values()) {
                String maStr = String.valueOf(v.getMa());
                if (maStr.equalsIgnoreCase(k)) return v;
            }
        } catch (Exception ignore) {}
        throw new IllegalArgumentException("Trạng thái tiếp theo không hợp lệ: " + key);
    }

    private boolean canTransit(TrangThaiChiTiet from, TrangThaiChiTiet to) {
        if (from == null) return true;
        return ALLOWED_TRANSITIONS.getOrDefault(from, Collections.emptySet()).contains(to);
    }

    private TrangThaiTong mapTongFromDetail(TrangThaiChiTiet detail, TrangThaiTong current) {
        if (detail == null) return current != null ? current : TrangThaiTong.DANG_XU_LY;
        switch (detail) {
            case HUY_DON:        return TrangThaiTong.DA_HUY;
            case HUY_GIAO_DICH:  return TrangThaiTong.HUY_GIAO_DICH;

            case DANG_GIAO_DICH:
            case CHO_XU_LY:
            case DA_XU_LY:
            case CHO_GIAO_HANG:
            case DANG_GIAO_HANG:
            case GIAO_THAT_BAI:
            case MAT_HANG:
                return TrangThaiTong.DANG_XU_LY;

            case YEU_CAU_HOAN:
                return TrangThaiTong.KHIEU_NAI;

            case DA_HOAN_TIEN:
                return TrangThaiTong.TRA_HANG;

            case GIAO_THANH_CONG:
            case DA_HOAN_THANH:
                return TrangThaiTong.THANH_CONG;

            default:
                return current != null ? current : TrangThaiTong.DANG_XU_LY;
        }
    }

    private String labelOf(TrangThaiChiTiet v) {
        try { return v.getMoTa(); } catch (Exception e) { return v.name(); }
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


    @Override
    @Transactional
    public void huyDonEmployee(Long invoiceId, String nextKey) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với username: " + username));

        Employee c = user.getEmployee();
        if (c == null) {
            throw new RuntimeException("Người dùng không phải là nhân viên.");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        TrangThaiChiTiet currentStatus = invoice.getStatusDetail();

        TrangThaiChiTiet nextStatus;
        try {
            nextStatus = TrangThaiChiTiet.valueOf(nextKey);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái tiếp theo không hợp lệ: " + nextKey);
        }

        if (currentStatus == nextStatus) {
            throw new RuntimeException("Trạng thái mới trùng với trạng thái hiện tại.");
        }

        invoice.setEmployee(c);
        invoice.setStatusDetail(nextStatus);

        if (nextStatus == TrangThaiChiTiet.HUY_DON) {
            invoice.setStatus(TrangThaiTong.DA_HUY);
        }

        invoiceRepository.save(invoice);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setInvoice(invoice);
        history.setEmployee(c);
        history.setOldStatus(currentStatus.getMa());
        history.setNewStatus(nextStatus.getMa());
        history.setChangedAt(new Date());

        historyRepository.save(history);
    }

    @Override
    @Transactional
    public void huyDonClient(Long invoiceId, String nextKey) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với username: " + username));

        Customer c = user.getCustomer();
        if (c == null) {
            throw new RuntimeException("Người dùng không phải là khách hàng.");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        TrangThaiChiTiet currentStatus = invoice.getStatusDetail();

        TrangThaiChiTiet nextStatus;
        try {
            nextStatus = TrangThaiChiTiet.valueOf(nextKey);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái tiếp theo không hợp lệ: " + nextKey);
        }

        if (currentStatus == nextStatus) {
            throw new RuntimeException("Trạng thái mới trùng với trạng thái hiện tại.");
        }

        invoice.setStatusDetail(nextStatus);

        if (nextStatus == TrangThaiChiTiet.HUY_DON) {
            invoice.setStatus(TrangThaiTong.DA_HUY);
        }
        invoice.setUpdatedDate(new Date()); // THÊM DÒNG NÀY
        invoiceRepository.save(invoice);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setCustomerId(c.getId());
        history.setInvoice(invoice);
        history.setOldStatus(currentStatus.getMa());
        history.setNewStatus(nextStatus.getMa());
        history.setChangedAt(new Date());

        historyRepository.save(history);

        // Gọi hàm kiểm tra và cấm nếu cần
        invoiceService.autoBlacklistIfTooManyCancellations(c);
    }

    @Override
    public InvoiceOnlineResponse getOrder(Long invoiceId) {
        InvoiceOnlineResponse response = invoiceRepository2.getOrder(invoiceId);
        List<InvoiceDetailOnline> invoiceDetailOnline = invoiceDetailRepository.findByInvoiceDetailOnline(invoiceId);
        List<InvoiceTransactionResponse> invoiceTransactionResponses = invoiceTransactionRepository.findInvoiceTransactionByIdInvoice(invoiceId);

        response.setInvoiceDetailResponses(invoiceDetailOnline);
        response.setInvoiceTransactionResponses(invoiceTransactionResponses);

        return response;
    }

    @Override
    @Transactional
    public void huyDonVaHoanTienClient(Long invoiceId, String nextKey, String note, Integer request, Boolean isPaid) {
        Invoice invoice = invoiceRepository.findPaidInvoiceById(invoiceId, isPaid)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        TrangThaiChiTiet statusDetail = invoice.getStatusDetail();
        if(statusDetail == TrangThaiChiTiet.DA_XU_LY) {
            List<InvoiceDetail> invoiceDetails = invoiceDetailRepository.findByInvoiceId(invoiceId);
            for (InvoiceDetail detail : invoiceDetails) {
                ProductDetail productDetail = detail.getProductDetail();

                int oldDetailStock = productDetail.getQuantity();
                productDetail.setQuantity(oldDetailStock + detail.getQuantity());
                productDetailRepository.save(productDetail);

                Product product = productDetail.getProduct();
                int oldProductStock = product.getQuantity();
                product.setQuantity(oldProductStock + detail.getQuantity());
                productRepository.save(product);
            }
        }

        huyDonClient(invoiceId, nextKey);

        InvoiceTransaction invoiceTransaction = new InvoiceTransaction();
        invoiceTransaction.setTransactionCode("GD-" + UUID.randomUUID().toString().substring(0, 8));
        invoiceTransaction.setInvoice(invoice);
        invoiceTransaction.setAmount(invoice.getFinalAmount());
        invoiceTransaction.setPaymentStatus(2);
        invoiceTransaction.setTransactionType("Yêu cầu hoàn tiền");
        invoiceTransaction.setNote(note);
        invoiceTransaction.setPaymentTime(new Date());
        invoiceTransactionRepository.save(invoiceTransaction);

        invoice.setRequest(request);
        Invoice saved = invoiceRepository.save(invoice);
        List<ReservationOrder> reservationOrder = reservationOrderRepository.findByInvoiceId(saved.getId());
        for(ReservationOrder order : reservationOrder) {
            order.setStatus(0);
        }
        reservationOrderRepository.saveAll(reservationOrder);
    }

    @Override
    @Transactional
    public void huyDonVaHoanTienEmployee(Long invoiceId, String nextKey, String note, String paymentMenthod, Boolean isPaid,String tradeCode,String bankName) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với username: " + username));

        Employee employee = user.getEmployee();
        if (employee == null) {
            throw new RuntimeException("Người dùng không phải là nhân viên.");
        }
        Invoice invoice = invoiceRepository.findPaidInvoiceById(invoiceId, isPaid)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        invoice.setEmployee(employee);

        TrangThaiChiTiet statusDetail = invoice.getStatusDetail();
        if(statusDetail == TrangThaiChiTiet.DA_XU_LY) {
            List<InvoiceDetail> invoiceDetails = invoiceDetailRepository.findByInvoiceId(invoiceId);
            for (InvoiceDetail detail : invoiceDetails) {
                ProductDetail productDetail = detail.getProductDetail();

                int oldDetailStock = productDetail.getQuantity();
                productDetail.setQuantity(oldDetailStock + detail.getQuantity());
                productDetailRepository.save(productDetail);

                Product product = productDetail.getProduct();
                int oldProductStock = product.getQuantity();
                product.setQuantity(oldProductStock + detail.getQuantity());
                productRepository.save(product);
            }
        }

        huyDonEmployee(invoiceId, nextKey);

        InvoiceTransaction invoiceTransaction = new InvoiceTransaction();
        invoiceTransaction.setTransactionCode("GD-" + UUID.randomUUID().toString().substring(0, 8));
        invoiceTransaction.setInvoice(invoice);
        invoiceTransaction.setAmount(invoice.getFinalAmount());
        invoiceTransaction.setPaymentStatus(2);
        invoiceTransaction.setTradeCode(tradeCode);
        invoiceTransaction.setBankName(bankName);
        invoiceTransaction.setPaymentMethod(paymentMenthod);
        invoiceTransaction.setTransactionType("Hoàn tiền");
        invoiceTransaction.setNote(note);
        invoiceTransaction.setPaymentTime(new Date());
        invoiceTransactionRepository.save(invoiceTransaction);

        Invoice saved = invoiceRepository.save(invoice);
        List<ReservationOrder> reservationOrder = reservationOrderRepository.findByInvoiceId(saved.getId());
        for(ReservationOrder order : reservationOrder) {
            order.setStatus(0);
        }
        reservationOrderRepository.saveAll(reservationOrder);
    }

    @Transactional
    public void giaoHangThatBaiEmployee(Long invoiceId, String nextKey, String note, String paymentMenthod, Boolean isPaid,String tradeCode,String bankName) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với username: " + username));

        Employee employee = user.getEmployee();
        if (employee == null) {
            throw new RuntimeException("Người dùng không phải là nhân viên.");
        }
        Invoice invoice = invoiceRepository.findPaidInvoiceById(invoiceId, isPaid)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        invoice.setEmployee(employee);
        huyDonEmployee(invoiceId, nextKey);

        TrangThaiChiTiet statusDetail = invoice.getStatusDetail();
        if(statusDetail == TrangThaiChiTiet.DANG_GIAO_HANG) {
            List<InvoiceDetail> invoiceDetails = invoiceDetailRepository.findByInvoiceId(invoiceId);
            for (InvoiceDetail detail : invoiceDetails) {
                ProductDetail productDetail = detail.getProductDetail();

                int oldDetailStock = productDetail.getQuantity();
                productDetail.setQuantity(oldDetailStock + detail.getQuantity());
                productDetailRepository.save(productDetail);

                Product product = productDetail.getProduct();
                int oldProductStock = product.getQuantity();
                product.setQuantity(oldProductStock + detail.getQuantity());
                productRepository.save(product);
            }
        }

        InvoiceTransaction invoiceTransaction = new InvoiceTransaction();
        invoiceTransaction.setTransactionCode("GD-" + UUID.randomUUID().toString().substring(0, 8));
        invoiceTransaction.setInvoice(invoice);
        invoiceTransaction.setAmount(invoice.getFinalAmount());
        invoiceTransaction.setPaymentStatus(2);
        invoiceTransaction.setTradeCode(tradeCode);
        invoiceTransaction.setBankName(bankName);
        invoiceTransaction.setPaymentMethod(paymentMenthod);
        invoiceTransaction.setTransactionType("Hoàn tiền");
        invoiceTransaction.setNote(note);
        invoiceTransaction.setPaymentTime(new Date());
        invoiceTransactionRepository.save(invoiceTransaction);

        Invoice saved = invoiceRepository.save(invoice);
        List<ReservationOrder> reservationOrder = reservationOrderRepository.findByInvoiceId(saved.getId());
        for(ReservationOrder order : reservationOrder) {
            order.setStatus(0);
        }
        reservationOrderRepository.saveAll(reservationOrder);
    }

    @Override
    public void giaoHangThatBaiVaHoanTien(Long invoiceId, String nextKey, String note, String paymentMenthod,Boolean isPaid,String tradeCode,String bankName) {
        giaoHangThatBaiEmployee(invoiceId,nextKey,note,paymentMenthod,isPaid,tradeCode,bankName);
    }

    @Override
    public List<StatusCountResponse> getCountByStatusDetail() {
        List<Object[]> result = invoiceRepository.countByStatusDetail();
        return result.stream()
                .map(obj -> new StatusCountResponse(((TrangThaiChiTiet) obj[0]).name(), (Long) obj[1]))
                .collect(Collectors.toList());
    }

    @Override
    public InvoiceOnlineResponse getOrderByCustomer(Long invoiceId) {

        InvoiceOnlineResponse response = invoiceRepository2.getOrder(invoiceId);
        if(response == null){
            return null;
        }
        List<InvoiceDetailOnline> invoiceDetailOnline = invoiceDetailRepository.findByInvoiceDetailOnline(invoiceId);
        List<InvoiceTransactionResponse> invoiceTransactionResponses = invoiceTransactionRepository.findInvoiceTransactionByIdInvoice(invoiceId);

        response.setInvoiceDetailResponses(invoiceDetailOnline);
        response.setInvoiceTransactionResponses(invoiceTransactionResponses);
        return response;
    }

    @Override
    public List<InvoiceOnlineResponse> getOrderByCustomer2(Integer status) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("user: "+username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với username: " + username));

        System.out.println("user: "+user.getCustomer().getCustomerName());

        Customer customer = user.getCustomer();
        if (customer == null) {
            throw new RuntimeException("Người dùng không phải là nhân viên.");
        }

        List<InvoiceOnlineResponse> response = invoiceRepository2.getOrder3(customer.getId(),status);
        if(response == null){
            return null;
        }
        return response;
    }

    @Override
    public List<InvoiceOnlineResponse> getOrderByCustomer3(Integer status) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("user: "+username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với username: " + username));

        System.out.println("user: "+user.getCustomer().getCustomerName());

        Customer customer = user.getCustomer();
        if (customer == null) {
            throw new RuntimeException("Người dùng không phải là nhân viên.");
        }

        List<InvoiceOnlineResponse> response = invoiceRepository2.getOrder4(customer.getId(),status);
        if(response == null){
            return null;
        }
        return response;
    }

    @Override
    public List<OrderStatusHistoryResponse> getOrderStatusHistory(Long invoiceId) {
        List<OrderStatusHistoryResponse> responses = historyRepository.getOrderStatusHistoriesByInvoice(invoiceId);
        System.out.println("response: "+responses);
        return responses;
    }

    @Override
    public void updateAddressShipping(UpdateAddress address) {
        Invoice invoice = invoiceRepository.findById(address.getInvoiceId())
                .orElseThrow(() -> new RuntimeException("Ko thấy hóa đơn với id: " + address.getInvoiceId()));
        invoice.setDeliveryAddress(address.getNewAddress());
        invoice.setShippingFee(address.getShippingFee());
        invoice.setFinalAmount(address.getFinalAmount());
        invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public void updateSDT(Long invoiceId, String phone) {
        if (invoiceId == null) {
            throw new IllegalArgumentException("invoiceId không được null.");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Ko thấy hóa đơn với id: " + invoiceId));

        invoice.setPhone(phone);
        invoiceRepository.save(invoice);
    }
    @Override
    public List<StatusCountDTO> getCountByStatus() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("user: "+username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với username: " + username));

        System.out.println("user: "+user.getCustomer().getCustomerName());

        Customer customer = user.getCustomer();
        if (customer == null) {
            throw new RuntimeException("Người dùng không phải là nhân viên.");
        }
        List<Object[]> results = invoiceRepository.countInvoicesByStatusNative(customer.getId());
        List<StatusCountDTO> list = new ArrayList<>();

        for (Object[] row : results) {
            String status = (String) row[0];
            Integer count = ((Number) row[1]).intValue();
            list.add(new StatusCountDTO(status, count));
        }
        return list;
    }

    @Override
    public List<StatusCountDTO> getCountByStatusV2() {
        List<Object[]> results = invoiceRepository.countInvoicesByStatusNativeV2();
        List<StatusCountDTO> list = new ArrayList<>();

        for (Object[] row : results) {
            String status = (String) row[0];
            Integer count = ((Number) row[1]).intValue();
            list.add(new StatusCountDTO(status, count));
        }
        return list;
    }

    @Override
    public BigDecimal getRevenue(String type) {
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);

        Date fromDate;
        Date toDate;

        switch (type.toLowerCase()) {
            case "day":
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                fromDate = calendar.getTime();

                calendar.add(Calendar.DATE, 1);
                toDate = calendar.getTime();
                break;

            case "week":
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                fromDate = calendar.getTime();

                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                toDate = calendar.getTime();
                break;

            case "quarter":
                int month = calendar.get(Calendar.MONTH);
                int quarterStartMonth = (month / 3) * 3;

                calendar.set(Calendar.MONTH, quarterStartMonth);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                fromDate = calendar.getTime();

                calendar.add(Calendar.MONTH, 3);
                toDate = calendar.getTime();
                break;

            case "year":
                calendar.set(Calendar.MONTH, Calendar.JANUARY);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                fromDate = calendar.getTime();

                calendar.add(Calendar.YEAR, 1);
                toDate = calendar.getTime();
                break;

            default:
                throw new IllegalArgumentException("Invalid type. Use: day|week|quarter|year");
        }
        BigDecimal result = invoiceRepository.sumRevenueBetween(fromDate, toDate);

        return  result != null ? result : BigDecimal.ZERO;
    }

}