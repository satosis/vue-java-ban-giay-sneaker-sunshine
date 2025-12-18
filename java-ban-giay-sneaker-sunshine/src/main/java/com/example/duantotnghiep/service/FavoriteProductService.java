package com.example.duantotnghiep.service;

import com.example.duantotnghiep.dto.request.FavoriteProductRequest;
import com.example.duantotnghiep.dto.response.FavoriteProductResponse;
import com.example.duantotnghiep.dto.response.RatingProductResponse;

import java.util.List;

public interface FavoriteProductService {
    FavoriteProductResponse addFavorite(FavoriteProductRequest request);
    void removeFavorite(Long customerId, Long productId);
    List<FavoriteProductResponse> getFavoritesByCustomer(Long customerId);
    List<RatingProductResponse> getFavoritesByCustomer();
}
