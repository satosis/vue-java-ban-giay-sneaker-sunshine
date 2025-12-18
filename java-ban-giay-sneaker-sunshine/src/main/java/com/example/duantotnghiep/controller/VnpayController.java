package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.request.InvoiceRequest;
import com.example.duantotnghiep.dto.response.InvoiceWithVnpayResponse;
import com.example.duantotnghiep.service.InvoiceService;
import com.example.duantotnghiep.service.impl.VnpayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment/vnpay")
public class VnpayController {

    private final VnpayService vnpayService;
    private final InvoiceService invoiceService;

    @PostMapping("/create")
    public ResponseEntity<?> createInvoiceWithVnpay(@RequestBody InvoiceRequest request) {
        try {
            InvoiceWithVnpayResponse response = invoiceService.createInvoiceAndVnpay(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi tạo hóa đơn: " + e.getMessage());
        }
    }

}

