package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.Product;
import com.example.duantotnghiep.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage,Long> {

    @Query("SELECT pi FROM ProductImage pi WHERE pi.id IN :ids AND pi.status = :status")
    List<ProductImage> findAllByIdAndStatus(@Param("ids") List<Long> ids, @Param("status") Integer status);

    @Query("""
    SELECT pi
    FROM ProductImage pi
    WHERE pi.product.id = :productId
      AND pi.color.id = :colorId
      AND pi.status = 1
""")
    List<ProductImage> findByProductIdAndColorId(
            @Param("productId") Long productId,
            @Param("colorId") Long colorId
    );


    @Query("SELECT pi FROM ProductImage pi WHERE pi.id IN :ids AND pi.status = :status")
    List<ProductImage> findAllByIdAndStatusRemoved(@Param("ids") List<Long> ids, @Param("status") Integer status);

    List<ProductImage> findByProduct(Product product);

    List<ProductImage> findByProductIdAndStatus(Long productId, Integer status);


}
