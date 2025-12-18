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
public class ProductAttributeResponse {
    private Long id;
    private String productName;
    private String productCode;
    private String categoryName;
    private SizeResponse size;
    private ColorResponse color;
    private BigDecimal price;
    private Integer quantity;
    private Integer availableQuantity;
}
