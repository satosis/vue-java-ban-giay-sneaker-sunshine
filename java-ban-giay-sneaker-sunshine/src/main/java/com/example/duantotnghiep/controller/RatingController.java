package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.response.TopRatedProductDTO;
import com.example.duantotnghiep.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/online-sale/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    // Lấy rating 1 sản phẩm
    // GET /api/online-sale/ratings/product/123
    @GetMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> getRatingByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ratingService.getRatingOfProduct(productId));
    }

    // Lấy rating nhiều sản phẩm (bulk) để tối ưu FE
    // GET /api/online-sale/ratings/products?ids=1,2,3
    @GetMapping("/products")
    public ResponseEntity<List<Map<String, Object>>> getRatingsByProducts(
            @RequestParam("ids") List<Long> ids
    ) {
        return ResponseEntity.ok(ratingService.getRatingsOfProducts(ids));
    }

    @GetMapping("/top5")
    public ResponseEntity<List<TopRatedProductDTO>> getTop5(
            @RequestParam(defaultValue = "0") long minReviews
    ) {
        return ResponseEntity.ok(ratingService.getTop5RatedWithSales(minReviews));
    }
}
