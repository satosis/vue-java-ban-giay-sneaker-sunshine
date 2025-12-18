package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RatingResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productCode;
    private String sizeName;
    private String colorName;
    private BigDecimal sellPrice;
    private Integer discountPercentage;
    private BigDecimal discountAmount;
    private BigDecimal discountedPrice;
    private Integer quantity;
    private String invoiceCodeDetail;
    private BigDecimal totalPrice;
    private BigDecimal finalTotalPrice;     // Tổng giá sau giảm = discountedPrice * quantity
    private String customerName;
    private byte[] image;
    private Integer rate;                  // NULL nếu chưa đánh giá
    private String comment;
    private boolean rated;
}
