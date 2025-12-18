package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.Product;
import com.example.duantotnghiep.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory,Long> {
    @Query("""
    select pc from ProductCategory pc where pc.product.id =:productId and pc.status = 1
""")
    List<ProductCategory> getAllByProductAndStatus(@Param("productId") Long productId);

    List<ProductCategory> findByProduct(Product product);

    @Query("""
        SELECT pc FROM ProductCategory pc
        LEFT JOIN FETCH pc.category
        WHERE pc.product.id = :productId AND pc.status = 1
    """)
    List<ProductCategory> findActiveByProductIdFetchCategory(@Param("productId") Long productId);
}
