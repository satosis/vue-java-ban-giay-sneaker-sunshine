package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.DiscountCampaignProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiscountCampaignProductDetailRepository extends JpaRepository<DiscountCampaignProductDetail, Long> {
    Optional<DiscountCampaignProductDetail> findFirstByCampaignIdAndProductDetailId(Long campaignId, Long productDetailId);

}