package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.DiscountCampaign;
import com.example.duantotnghiep.model.DiscountCampaignProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DiscountCampaignProductRepository extends JpaRepository<DiscountCampaignProduct, Long> {
    boolean existsByCampaign_IdAndProduct_Id(Long campaignId, Long productId);

    @Query("""
                SELECT dcp.campaign 
                FROM DiscountCampaignProduct dcp
                JOIN dcp.product p
                JOIN ProductDetail pd ON pd.product = p
                WHERE pd.id = :productDetailId
                  AND dcp.campaign.startDate <= :now
                  AND dcp.campaign.endDate >= :now
                  AND dcp.campaign.status = 1
            """)
    List<DiscountCampaign> findActiveCampaignsByProductDetailId(
            @Param("productDetailId") Long productDetailId,
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Query("DELETE FROM DiscountCampaignProduct d WHERE d.campaign.id = :campaignId")
    void deleteByCampaignId(@Param("campaignId") Long campaignId);


}