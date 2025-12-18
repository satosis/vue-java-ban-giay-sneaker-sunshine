package com.example.duantotnghiep.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DiscountCampaignProductRequest {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal discountAmount;
}

