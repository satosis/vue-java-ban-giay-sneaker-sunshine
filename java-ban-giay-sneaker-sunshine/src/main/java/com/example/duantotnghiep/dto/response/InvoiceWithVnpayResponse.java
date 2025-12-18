package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceWithVnpayResponse {
    private InvoiceDisplayResponse invoiceData;
    private VnpayResponse vnpay;
}

