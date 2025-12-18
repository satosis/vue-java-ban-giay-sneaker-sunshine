package com.example.duantotnghiep.service;

import com.example.duantotnghiep.dto.response.ProductImageResponse;

import java.util.List;

public interface ProductImageService {
    List<ProductImageResponse> getImagesByProductAndColor(Long productId, Long colorId);
}
