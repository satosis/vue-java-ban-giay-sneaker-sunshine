package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.request.FavoriteProductRequest;
import com.example.duantotnghiep.dto.response.FavoriteProductResponse;
import com.example.duantotnghiep.dto.response.RatingProductResponse;
import com.example.duantotnghiep.service.FavoriteProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteProductController {

    private final FavoriteProductService service;

    @PostMapping
    public FavoriteProductResponse addFavorite(@RequestBody FavoriteProductRequest request) {
        return service.addFavorite(request);
    }

    @DeleteMapping
    public void removeFavorite(@RequestParam Long customerId, @RequestParam Long productId) {
        service.removeFavorite(customerId, productId);
    }

    @GetMapping("/{customerId}")
    public List<FavoriteProductResponse> getFavorites(@PathVariable Long customerId) {
        return service.getFavoritesByCustomer(customerId);
    }

    @GetMapping("/san-pham-da-mua")
    public List<RatingProductResponse> getRatingProducts() {
        return service.getFavoritesByCustomer();
    }

}
