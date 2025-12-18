package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RatingProductResponse {
    private Long customerId;
    private Long productId;
    private String productName;
    private byte[] image;
    private Boolean isRated;
}
