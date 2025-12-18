package com.example.duantotnghiep.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseStats {
    private BigDecimal totalSpent;
    private int invoiceCount;
}

