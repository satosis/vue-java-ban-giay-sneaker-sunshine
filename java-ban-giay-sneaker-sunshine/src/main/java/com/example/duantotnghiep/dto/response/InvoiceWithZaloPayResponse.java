package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceWithZaloPayResponse {
    private InvoiceDisplayResponse invoiceData;
    private ZaloPayResponse zaloPay;
}

