package com.example.duantotnghiep.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class DiscountCampaignRequest {
    private String campaignCode;
    private String name;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer status;
    private BigDecimal discountPercentage;

    private List<DiscountCampaignProductRequest> products;
    private List<DiscountCampaignProductDetailRequest> productDetails;
}


