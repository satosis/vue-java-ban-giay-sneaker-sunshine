package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DiscountCampaignResponse {
    private Long id;
    private String campaignCode;
    private String name;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer status;
    private BigDecimal discountPercentage;
    private List<DiscountCampaignProductResponse> products;
    private List<DiscountCampaignProductDetailResponse> productDetails;

}

