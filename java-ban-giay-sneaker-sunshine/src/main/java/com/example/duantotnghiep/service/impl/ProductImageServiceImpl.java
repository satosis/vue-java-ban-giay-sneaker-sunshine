package com.example.duantotnghiep.service.impl;
import com.example.duantotnghiep.dto.response.ProductImageResponse;
import com.example.duantotnghiep.mapper.ProductImageMapper;
import com.example.duantotnghiep.model.ProductImage;
import com.example.duantotnghiep.repository.ProductImageRepository;
import com.example.duantotnghiep.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl implements ProductImageService {
    private final ProductImageRepository productImageRepository;
    private final ProductImageMapper productImageMapper;

    @Override
    public List<ProductImageResponse> getImagesByProductAndColor(Long productId, Long colorId) {
        List<ProductImage> images = productImageRepository.findByProductIdAndColorId(productId, colorId);
        return productImageMapper.toResponseList(images);
    }
}
