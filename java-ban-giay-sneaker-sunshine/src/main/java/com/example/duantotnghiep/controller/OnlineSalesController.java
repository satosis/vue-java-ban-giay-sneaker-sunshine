package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.request.InvoiceSearchRequest;
import com.example.duantotnghiep.dto.request.UpdateAddress;
import com.example.duantotnghiep.dto.response.*;
import com.example.duantotnghiep.service.InvoiceService;
import com.example.duantotnghiep.service.impl.OnlineSaleServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/online-sales")
@RequiredArgsConstructor
public class OnlineSalesController {
    private final InvoiceService invoiceService;
    private final OnlineSaleServiceImpl onlineSaleService;

    @PostMapping("/search")
    public ResponseEntity<List<InvoiceResponse>> searchInvoices(
            @RequestBody InvoiceSearchRequest request
            ) {
        List<InvoiceResponse> result = invoiceService.searchInvoices(request);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/chuyen-trang-thai")
    public ResponseEntity<String> chuyenTrangThai(@RequestParam("invoiceId") Long invoiceId,@RequestParam("statusDetail") String nextKey) {
        try {
            onlineSaleService.chuyenTrangThai(invoiceId,nextKey);
            return ResponseEntity.ok("Chuyển trạng thái đơn hàng thành công.");
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body("Lỗi: " + ex.getMessage());
        }
    }

    @GetMapping("/get-order")
    public ResponseEntity<InvoiceOnlineResponse> getOrderOnline(@RequestParam("invoiceId") Long invoiceId){
        InvoiceOnlineResponse response = onlineSaleService.getOrder(invoiceId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/huy-don-va-hoan-tien")
    public ResponseEntity<?> huyDonVaHoanTien(
            @RequestParam Long invoiceId,
            @RequestParam String statusDetail,
            @RequestParam String note,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String tradeCode,
            @RequestParam(required = false) String bankName,
            @RequestParam Boolean isPaid
    ) {
        try {
            onlineSaleService.huyDonVaHoanTienEmployee(invoiceId, statusDetail, note, paymentMethod,isPaid,tradeCode,bankName);
            return ResponseEntity.ok("Hủy đơn và hoàn tiền thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Đã xảy ra lỗi máy chủ");
        }
    }

    @PutMapping("/failed-shipping")
    public ResponseEntity<?> giaoHangThatBaiVaHoanTien(
            @RequestParam Long invoiceId,
            @RequestParam String statusDetail,
            @RequestParam String note,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String tradeCode,
            @RequestParam(required = false) String bankName,
            @RequestParam Boolean isPaid
    ) {
        try {
            onlineSaleService.giaoHangThatBaiVaHoanTien(invoiceId, statusDetail, note, paymentMethod,isPaid,tradeCode,bankName);
            return ResponseEntity.ok("Giao hàng thất bại và hoàn tiền thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Đã xảy ra lỗi máy chủ");
        }
    }

    @GetMapping("/count-by-status")
    public ResponseEntity<List<StatusCountDTO>> getCountByStatus() {
        return ResponseEntity.ok(onlineSaleService.getCountByStatusV2());
    }

    @GetMapping("/get-order-customer")
    public ResponseEntity<List<InvoiceOnlineResponse>> getOrderCustomerOnline2(@RequestParam("statusDetail") Integer statusDetail){
        System.out.println("status: "+statusDetail);
        List<InvoiceOnlineResponse> response = onlineSaleService.getOrderByCustomer2(statusDetail);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get-order-customer-detail")
    public ResponseEntity<InvoiceOnlineResponse> getOrderCustomerOnline(@RequestParam("invoiceId") Long invoiceId){
        InvoiceOnlineResponse response = onlineSaleService.getOrderByCustomer(invoiceId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get-order-history")
    public ResponseEntity<List<OrderStatusHistoryResponse>> getOrderHistory(@Param("invoiceId") Long invoiceId){
        List<OrderStatusHistoryResponse> list = onlineSaleService.getOrderStatusHistory(invoiceId);
        return ResponseEntity.ok(list);
    }

    @PutMapping("/update-address")
    public ResponseEntity<String> capNhatDiaChi(
            @RequestBody UpdateAddress request
            ) {
        onlineSaleService.updateAddressShipping(request);
        return ResponseEntity.ok("Cập nhật địa chỉ giao hàng thành công.");
    }

    @PutMapping("/update-phone")
    public ResponseEntity<String> capNhatSDT(
            @RequestParam("invoiceId") Long invoiceId,
            @RequestParam("phone") String phone
    ) {
        onlineSaleService.updateSDT(invoiceId,phone);
        return ResponseEntity.ok("Cập nhật sdt thành công.");
    }

    @GetMapping("/get-revenue")
    public ResponseEntity<?> getRevenue(@RequestParam String type) {
        BigDecimal revenue = onlineSaleService.getRevenue(type);
        return ResponseEntity.ok(Map.of(
                "type", type,
                "revenue", revenue
        ));
    }

}
