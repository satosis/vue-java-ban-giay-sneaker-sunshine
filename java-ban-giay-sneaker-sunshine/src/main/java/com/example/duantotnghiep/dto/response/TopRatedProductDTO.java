package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TopRatedProductDTO {
    private Long productId;
    private String productName;
    private String sku;

    private Double avgRating;        // Trung bình rating (1..5)
    private Long reviewCount;        // Số bản ghi có rate != null

    private Long favoritesCount;     // Tổng lượt yêu thích (status = 1)
    private Long totalQuantitySold;  // Tổng SL đã bán
}
