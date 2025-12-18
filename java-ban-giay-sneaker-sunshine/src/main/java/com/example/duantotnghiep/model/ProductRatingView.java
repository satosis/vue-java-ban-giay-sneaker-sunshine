package com.example.duantotnghiep.model;

public interface ProductRatingView {
    Long getProductId();
    Double getAvgRating();     // trung bình sao (1..5)
    Long getTotalReviews();    // tổng lượt đánh giá
    Long getStar1();
    Long getStar2();
    Long getStar3();
    Long getStar4();
    Long getStar5();
}

