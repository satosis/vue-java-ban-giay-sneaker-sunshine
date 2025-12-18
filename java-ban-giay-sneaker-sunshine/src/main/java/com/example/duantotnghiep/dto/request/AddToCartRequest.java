package com.example.duantotnghiep.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AddToCartRequest {
    private Long invoiceId;
    private Long productDetailId;
    private Integer quantity;
    private Integer discountPercentage; // ⭐ FE gửi lên
    private Long discountCampaignId; // thêm mới

}
