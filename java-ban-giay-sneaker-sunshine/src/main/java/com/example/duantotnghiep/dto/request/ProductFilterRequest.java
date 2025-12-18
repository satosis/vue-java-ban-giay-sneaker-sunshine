package com.example.duantotnghiep.dto.request;

import lombok.Data;

@Data
public class ProductFilterRequest {
    private Long brandId;
    private Long categoryId;
    private Long genderId;
    private Long colorId;
    private Long sizeId;
}

