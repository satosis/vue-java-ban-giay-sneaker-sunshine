package com.example.duantotnghiep.service;

import com.example.duantotnghiep.dto.response.FavouriteResponse;
import com.example.duantotnghiep.dto.response.TopRatedProductDTO;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface RatingService {
    Map<String, Object> getRatingOfProduct(Long productId);

    List<Map<String, Object>> getRatingsOfProducts(List<Long> productIds);

    List<FavouriteResponse> getDeliveredInvoicesForReview(Integer onlyUnrated,
                                                          String keyword,
                                                          Date dateFrom,
                                                          Date dateTo);


    List<TopRatedProductDTO> getTop5RatedWithSales(long minReviews);
}
