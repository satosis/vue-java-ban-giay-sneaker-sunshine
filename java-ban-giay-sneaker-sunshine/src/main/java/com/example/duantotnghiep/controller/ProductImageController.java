package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.response.ProductImageResponse;
import com.example.duantotnghiep.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/product-images")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService productImageService;

    @GetMapping
    public ResponseEntity<List<ProductImageResponse>> getImagesByProductAndColor(
            @RequestParam Long productId,
            @RequestParam Long colorId) {
        List<ProductImageResponse> responses = productImageService.getImagesByProductAndColor(productId, colorId);
        return ResponseEntity.ok(responses);
    }
}

