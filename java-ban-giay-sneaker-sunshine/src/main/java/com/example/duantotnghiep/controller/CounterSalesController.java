package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.PaymentSummary;
import com.example.duantotnghiep.dto.request.AddToCartRequest;
import com.example.duantotnghiep.dto.request.AssignCustomerRequest;
import com.example.duantotnghiep.dto.response.CustomerResponse;
import com.example.duantotnghiep.dto.response.InvoiceDisplayResponse;
import com.example.duantotnghiep.dto.response.InvoiceResponse;
import com.example.duantotnghiep.dto.response.ProductAttributeResponse;
import com.example.duantotnghiep.dto.response.VoucherResponse;
import com.example.duantotnghiep.mapper.VoucherMapper;
import com.example.duantotnghiep.model.Invoice;
import com.example.duantotnghiep.service.impl.InvoiceServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/counter-sales")
@RequiredArgsConstructor
@Validated
public class CounterSalesController {

    private final InvoiceServiceImpl invoiceService;
    private final VoucherMapper voucherMapper;


    @GetMapping("/invoices")
    public ResponseEntity<Page<InvoiceResponse>> getInvoicesByStatus(
            @RequestParam int status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Page<InvoiceResponse> result = invoiceService.getInvoicesByStatus(status, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{invoiceId}/details")
    public ResponseEntity<InvoiceDisplayResponse> getInvoiceDetails(@PathVariable Long invoiceId) {
        InvoiceDisplayResponse response = invoiceService.getInvoiceWithDetails(invoiceId);
        System.out.println("Customer ID in invoice: " + response.getInvoice().getCustomerId());
        return ResponseEntity.ok(response);
    }

    // API thêm chi tiết hóa đơn (POST)
    @PostMapping("/{invoiceId}/details")
    public ResponseEntity<?> addInvoiceDetail(
            @PathVariable Long invoiceId,
            @RequestBody AddToCartRequest request) {
        try {
            invoiceService.addInvoiceDetails(
                    invoiceId,
                    request.getProductDetailId(),
                    request.getQuantity(),
                    request.getDiscountPercentage() != null ? request.getDiscountPercentage() : 0,
                    request.getDiscountCampaignId() // truyền thêm id chiến dịch giảm giá
            );
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Thêm sản phẩm vào hóa đơn thất bại: " + e.getMessage());
        }
    }

    @PutMapping("/invoice-details/{id}/quantity")
    public ResponseEntity<InvoiceDisplayResponse> updateQuantity(
            @PathVariable("id") Long invoiceDetailId,
            @RequestParam("quantity") Integer newQuantity) {
        InvoiceDisplayResponse response = invoiceService.updateInvoiceDetailQuantity(invoiceDetailId, newQuantity);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}/attributes")
    public ResponseEntity<List<ProductAttributeResponse>> getProductAttributes(@PathVariable Long productId) {
        List<ProductAttributeResponse> response = invoiceService.getProductAttributesByProductId(productId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-empty")
    public ResponseEntity<InvoiceResponse> createEmptyInvoice() {
        InvoiceResponse response = invoiceService.createEmptyInvoice();
        return ResponseEntity.ok(response);
    }

    /**
     * Tìm hoặc tạo mới nhanh khách hàng theo số điện thoại
     */
    @GetMapping("/search-by-phone-prefix")
    public ResponseEntity<List<CustomerResponse>> searchByPhonePrefix(@RequestParam String phone) {
        List<CustomerResponse> list = invoiceService.findCustomersByPhonePrefix(phone);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/quick-create-customer")
    public ResponseEntity<CustomerResponse> createQuickCustomer(
            @RequestParam String phone,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email) {

        CustomerResponse response = invoiceService.createQuickCustomer(phone, name, email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{invoiceId}/assign-customer")
    public ResponseEntity<?> assignCustomerToInvoice(
            @PathVariable Long invoiceId,
            @RequestBody AssignCustomerRequest request) {

        try {
            invoiceService.assignCustomer(invoiceId, request.getCustomerId());
            return ResponseEntity.ok("Gán khách hàng thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi server khi gán khách hàng");
        }
    }

    /**
     * Tính tiền cần trả và tiền thừa
     */
    @GetMapping("/{invoiceId}/calculate-payment")
    public ResponseEntity<?> calculatePayment(
            @PathVariable Long invoiceId,
            @RequestParam BigDecimal amountGiven) {
        try {
            PaymentSummary summary = invoiceService.calculatePayment(invoiceId, amountGiven);
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            // Kiểm tra nếu lỗi do tiền đưa không đủ
            if ("Số tiền đưa không đủ".equals(e.getMessage())) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", e.getMessage()));
            }
            // Các lỗi khác trả 500
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi máy chủ, vui lòng thử lại"));
        }
    }

    /**
     * Thanh toán và hủy hóa đơn
     */
    @PostMapping("/{invoiceId}/cancel")
    public ResponseEntity<String> cancelInvoice(@PathVariable Long invoiceId) {
        try {
            invoiceService.cancelInvoice(invoiceId);
            return ResponseEntity.ok("Hủy hóa đơn thành công");
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi server");
        }
    }

    @PostMapping("/{invoiceId}/checkout")
    public ResponseEntity<?> checkoutInvoice(@PathVariable Long invoiceId) {
        try {
            invoiceService.checkout(invoiceId);
            return ResponseEntity.ok(Map.of("message", "Thanh toán thành công"));
        } catch (RuntimeException ex) {
            // Trả về lỗi rõ ràng cho FE đọc được: err.response.data.message
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    /**
     * Xóa giỏ hàng (hóa đơn) theo ID
     */
    @DeleteMapping("/{invoiceId}/clear-cart")
    public ResponseEntity<?> clearCart(@PathVariable Long invoiceId) {
        invoiceService.clearCart(invoiceId);
        return ResponseEntity.ok("Giỏ hàng đã được xóa");
    }

    @DeleteMapping("/cart-item/{invoiceDetailId}")
    public ResponseEntity<?> deleteCartItem(@PathVariable Long invoiceDetailId) {
        invoiceService.deleteCartItemById(invoiceDetailId);
        return ResponseEntity.ok("Đã xóa sản phẩm trong giỏ");
    }

    @PostMapping("/{invoiceId}/apply-voucher")
    public ResponseEntity<?> applyVoucher(
            @PathVariable Long invoiceId,
            @RequestParam String voucherCode) {
        try {
            Invoice updatedInvoice = invoiceService.applyVoucherToInvoice(invoiceId, voucherCode);

            if (updatedInvoice.getVoucher() == null) {
                return ResponseEntity.badRequest()
                        .body("Không thể áp dụng voucher.");
            }

            // Trả kèm thông tin hóa đơn và voucher để FE cập nhật
            Map<String, Object> response = new HashMap<>();
            response.put("voucher", voucherMapper.toDto(updatedInvoice.getVoucher()));
            response.put("finalAmount", updatedInvoice.getFinalAmount());
            response.put("discountAmount", updatedInvoice.getDiscountAmount());

            return ResponseEntity.ok(response);

        } catch (RuntimeException ex) {
            // Trả thẳng message từ service (bao gồm cả min order)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi hệ thống khi áp dụng voucher.");
        }
    }


    @PostMapping("/{invoiceId}/apply-best-voucher")
    public ResponseEntity<?> applyBestVoucher(@PathVariable Long invoiceId) {
        try {
            Invoice updatedInvoice = invoiceService.applyBestVoucherToInvoice(invoiceId);

            if (updatedInvoice.getVoucher() == null) {
                return ResponseEntity.ok("Không có voucher nào phù hợp để áp dụng.");
            }

            VoucherResponse response = voucherMapper.toDto(updatedInvoice.getVoucher());
            return ResponseEntity.ok(response);

        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest()
                    .body("Lỗi khi áp dụng voucher: " + ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi hệ thống khi áp dụng voucher.");
        }
    }

    @PutMapping("/{invoiceId}/remove-voucher")
    public ResponseEntity<?> removeVoucher(@PathVariable Long invoiceId) {
        try {
            Invoice updatedInvoice = invoiceService.removeVoucherFromInvoice(invoiceId);

            return ResponseEntity.ok("Đã bỏ áp dụng voucher. Tổng tiền: " + updatedInvoice.getFinalAmount());

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Lỗi khi bỏ voucher: " + ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi hệ thống khi bỏ voucher.");
        }
    }

}

