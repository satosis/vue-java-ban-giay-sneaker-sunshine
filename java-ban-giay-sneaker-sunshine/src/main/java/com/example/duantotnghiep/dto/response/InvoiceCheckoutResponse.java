package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InvoiceCheckoutResponse {
    private Long invoiceId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private String voucherCode;
    private BigDecimal finalAmount;
}
