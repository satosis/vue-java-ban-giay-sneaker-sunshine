package com.example.duantotnghiep.dto.response;

import lombok.Data;

@Data
public class DiscountCampaignProductResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productCode; // <<-- mới thêm

}
