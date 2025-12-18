package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.ProductDetail;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductDetailRepository extends JpaRepository<ProductDetail,Long> {
    @Query("SELECT pd FROM ProductDetail pd WHERE pd.product.id = :productId AND pd.status = :status")
    List<ProductDetail> findByProductIdAndStatus(@Param("productId") Long productId, @Param("status") Integer status);

    @Query("SELECT pd FROM ProductDetail pd WHERE pd.product.id = :productId AND pd.status = :status")
    List<ProductDetail> findByProductIdAndStatusRemoved(@Param("productId") Long productId, @Param("status") Integer status);

    List<ProductDetail> findByProductId(Long productId);

    List<ProductDetail> findByProductIdAndColorId(Long productId, Long colorId);


    @Query("""
        SELECT pd FROM ProductDetail pd
        JOIN FETCH pd.product p
        LEFT JOIN FETCH pd.color
        LEFT JOIN FETCH pd.size
        WHERE pd.productDetailCode = :code AND pd.status = 1 AND p.status = 1
    """)
    Optional<ProductDetail> findActiveByDetailCode(@Param("code") String code);

    @Query("""
        SELECT pd FROM ProductDetail pd
        LEFT JOIN FETCH pd.color
        LEFT JOIN FETCH pd.size
        WHERE pd.product.id = :productId AND pd.status = 1
    """)
    List<ProductDetail> findActiveByProductIdFetchAttrs(@Param("productId") Long productId);

    @Query("""
    SELECT pd
    FROM ProductDetail pd
    WHERE pd.status = 1
    ORDER BY pd.id DESC
""")
    Page<ProductDetail> pageAllActive(Pageable pageable);

    @Query("""
    SELECT pd
    FROM ProductDetail pd
    WHERE pd.status = 1
      AND (:productIds IS NULL OR pd.product.id IN :productIds)
    ORDER BY pd.id DESC
""")
    Page<ProductDetail> pageAllActive(@Param("productIds") List<Long> productIds, Pageable pageable);

    @Query("""
    SELECT pd
    FROM ProductDetail pd
    WHERE pd.status = 1
      AND (:productId IS NULL OR pd.product.id = :productId)
      AND (:colorId   IS NULL OR pd.color.id = :colorId)
      AND (:brandId   IS NULL OR pd.product.brand.id = :brandId)
    ORDER BY pd.id DESC
""")
    Page<ProductDetail> pageByProductId(
            @Param("productId") Long productId,
            @Param("colorId") Long colorId,
            @Param("brandId") Long brandId,
            Pageable pageable
    );

    @Query("""
        select pd from ProductDetail pd
        left join fetch pd.product p
        left join fetch p.brand pb
        left join fetch pd.size s
        left join fetch pd.color c
        where pd.productDetailCode = :code
          and pd.status = 1
    """)
    Optional<ProductDetail> findActiveByDetailCodeFetchAll(@Param("code") String code);

    @Query("SELECT pd FROM ProductDetail pd WHERE pd.id in :ids")
    List<ProductDetail> findByProductIds(@Param("ids") List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pd from ProductDetail pd join fetch pd.product where pd.id = :id")
    Optional<ProductDetail> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select pd from ProductDetail pd where pd.id = :id and pd.status = 1
    """)
    Optional<ProductDetail> findByIdAndStatus(@Param("id") Long id);


}
