package com.example.duantotnghiep.model;

import com.example.duantotnghiep.state.CustomerTier;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PromotionSuggestion {
    private CustomerTier tier;
    private String message;
    private String couponCode;
    private BigDecimal discountAmount;
    private int invoiceCount;
}

