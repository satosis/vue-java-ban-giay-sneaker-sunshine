package com.example.duantotnghiep.dto.response;

import lombok.Data;

@Data
public class DiscountCampaignProductDetailResponse {
    private Long id;
    private Long productDetailId;

    private String productName;
    private String productCode;
    private String colorName;
    private String sizeName;

    private java.math.BigDecimal sellPrice;
    private java.math.BigDecimal discountPercent;
    private Long stock;
}

