package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.dto.response.DiscountCampaignResponse;
import com.example.duantotnghiep.model.DiscountCampaign;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DiscountCampaignRepository extends JpaRepository<DiscountCampaign, Long> {
    boolean existsByCampaignCode(@Size(max = 100) String campaignCode);
//    @Query("SELECT c FROM DiscountCampaign c WHERE c.startDate <= :now AND c.endDate >= :now AND c.status = 1")
//    List<DiscountCampaign> findActiveCampaigns(@Param("now") LocalDateTime now);

    @Query("""
    select dc 
    from DiscountCampaign dc
    where dc.status in (0,1)
      and (:id is null or dc.id <> :id)
      and (dc.startDate <= :endDate and dc.endDate >= :startDate)
    """)
    List<DiscountCampaign> findConflictCampaign(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("id") Long id);



@Query("SELECT d FROM DiscountCampaign d WHERE d.status IN (0,1)")
Page<DiscountCampaign> findActiveCampaigns(Pageable pageable);

    @Query("SELECT c FROM DiscountCampaign c WHERE c.startDate > :now AND c.status = 0")
    List<DiscountCampaign> findUpcomingCampaigns(@Param("now") LocalDateTime now);

    List<DiscountCampaign> findByStatusNotAndEndDateBefore(Integer status, LocalDateTime now);

    List<DiscountCampaign> findAllByEndDateBeforeAndStatusNot(LocalDateTime endDate, Integer status);

    @Query("""
                SELECT c FROM DiscountCampaign c
                WHERE c.status = 1
                  AND (c.startDate IS NULL OR c.startDate <= :now)
                  AND (c.endDate   IS NULL OR c.endDate   >= :now)
            """)
    List<DiscountCampaign> findActiveCampaignsQr(@Param("now") LocalDateTime now);

    @Query(
            "SELECT DISTINCT c " +
                    "FROM DiscountCampaign c " +
                    "LEFT JOIN FETCH c.productDetails dcpd " +         // fetch 1 collection: OK
                    "LEFT JOIN FETCH dcpd.productDetail pd " +         // fetch đơn trị: OK
                    "WHERE c.status = 1 " +
                    "  AND (c.startDate IS NULL OR c.startDate <= :now) " +
                    "  AND (c.endDate   IS NULL OR c.endDate   >= :now)"
    )
    List<DiscountCampaign> findActiveCampaigns(@Param("now") LocalDateTime now);

    @Query("""
            SELECT new com.example.duantotnghiep.dto.response.DiscountCampaignResponse(
                c.id,
                c.campaignCode,
                c.name,
                c.description,
                c.startDate,
                c.endDate,
                c.status,
                c.discountPercentage,
                null,
                null
            )
            FROM DiscountCampaign c
            WHERE
              ( :keyword IS NULL OR :keyword = '' OR
                LOWER(c.name)         LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(c.campaignCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(c.description)  LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
              AND ( :status IS NULL OR c.status = :status )
              AND (
                   (:createdStart IS NULL AND :createdEnd IS NULL)
                OR (:createdStart IS NOT NULL AND :createdEnd IS NULL AND c.createdDate >= :createdStart)
                OR (:createdStart IS NULL AND :createdEnd IS NOT NULL AND c.createdDate <= :createdEnd)
                OR (:createdStart IS NOT NULL AND :createdEnd IS NOT NULL AND c.createdDate BETWEEN :createdStart AND :createdEnd)
              )
            ORDER BY c.createdDate DESC
            """)
    Page<DiscountCampaignResponse> searchByKeywordStatusCreatedDate(
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("createdStart") LocalDateTime createdStart,
            @Param("createdEnd") LocalDateTime createdEnd,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT c
            FROM DiscountCampaign c
            JOIN c.productDetails d
            WHERE c.status = 1
              AND (c.startDate IS NULL OR :now >= c.startDate)
              AND (c.endDate   IS NULL OR :now <= c.endDate)
              AND d.productDetail.id = :pdId
            """)
    List<DiscountCampaign> findActiveForProductDetail(@Param("pdId") Long pdId,
                                                      @Param("now") LocalDateTime now);

    @Query("""
            SELECT DISTINCT c
            FROM DiscountCampaign c
            JOIN c.products p
            WHERE c.status = 1
              AND (c.startDate IS NULL OR :now >= c.startDate)
              AND (c.endDate   IS NULL OR :now <= c.endDate)
              AND p.product.id = :productId
            """)
    List<DiscountCampaign> findActiveForProduct(@Param("productId") Long productId,
                                                @Param("now") LocalDateTime now);

    List<DiscountCampaign> findAllByStartDateLessThanEqualAndStatus(LocalDateTime now, int status);

    @Query("""
        select distinct dc
        from DiscountCampaign dc
        left join fetch dc.productDetails dcpd
        left join fetch dcpd.productDetail pd
        left join fetch pd.product p
        left join fetch pd.color c
        left join fetch pd.size s
        where dc.id = :id
    """)
    Optional<DiscountCampaign> findDetailGraph(@Param("id") Long id);

}