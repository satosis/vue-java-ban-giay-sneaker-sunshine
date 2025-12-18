package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.id = :id and p.status = 1")
    Optional<Product> findByStatus(@Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE p.id = :id and p.status = 0")
    Optional<Product> findByStatusRemoved(@Param("id") Long id);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.productDetails WHERE p.id = :id and p.status = 1 ")
    Optional<Product> findByIdWithProductDetails(@Param("id") Long id);


    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.productImages WHERE p.id = :id")
    Optional<Product> findByIdWithProductImages(@Param("id") Long id);

    @Query("SELECT p FROM Product p " +
            "LEFT JOIN FETCH p.productCategories pc " +
            "LEFT JOIN FETCH pc.category " +
            "WHERE p.id = :id")
    Optional<Product> findByIdWithCategories(@Param("id") Long id);

    @Query("SELECT p FROM Product p " +
            "LEFT JOIN FETCH p.productImages pi  where p.status = 1 and pi.status = 1 order by p.createdDate desc ")
    List<Product> findProductWithImage(Pageable pageable);

    @Query("""
  SELECT DISTINCT p
  FROM Product p
  LEFT JOIN FETCH p.productImages pi
  WHERE p.status = 1
    AND (pi.status = 1 OR pi.id IS NULL)
    AND (
      :kw IS NULL OR :kw = '' OR
      LOWER(p.productName) LIKE LOWER(CONCAT('%', :kw, '%'))
    )
  ORDER BY p.createdDate DESC
""")
    List<Product> findProductWithImageV2(@Param("kw") String kw);


    @Query("SELECT p FROM Product p where p.status = 1 order by p.createdDate desc ")
    List<Product> findAllWithJPQL();

    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.material " +
            "LEFT JOIN FETCH p.brand " +
            "LEFT JOIN FETCH p.style " +
            "LEFT JOIN FETCH p.gender " +
            "LEFT JOIN FETCH p.sole " +
            "LEFT JOIN FETCH p.supplier " +
            "LEFT JOIN FETCH p.productCategories pc " +
            "LEFT JOIN FETCH pc.category " +
            "WHERE p.id IN :ids")
    List<Product> findByIdsWithCategories(@Param("ids") List<Long> ids);

    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.material " +
            "LEFT JOIN FETCH p.brand " +
            "LEFT JOIN FETCH p.style " +
            "LEFT JOIN FETCH p.gender " +
            "LEFT JOIN FETCH p.sole " +
            "LEFT JOIN FETCH p.supplier " +
            "LEFT JOIN FETCH p.productDetails pd " +
            "WHERE p.id IN :ids")
    List<Product> findByIdsWithDetails(@Param("ids") List<Long> ids);

    @Query("SELECT p FROM Product p WHERE p.status = 1 order by p.createdDate desc ")
    Page<Product> findAllWithJPQL(Pageable pageable);

    Optional<Product> findTop1ByProductCodeAndStatus(String productCode, int status);

    @Query("SELECT p FROM Product p WHERE p.id in :ids and p.status = 1 order by p.createdDate desc ")
    List<Product> findAllByIds(List<Long> ids);

    @Query("""
                select p from Product p
                join fetch p.material
                join fetch p.brand
                join fetch p.style
                join fetch p.gender
                join fetch p.sole
                join fetch p.supplier
                where coalesce(p.status, 1) = 1 
                  and p.brand.id = :brandId
                order by p.createdDate desc
            """)
    Page<Product> findAllByBrand(@Param("brandId") Long brandId, Pageable pageable);


    @Query("""
            SELECT p
            FROM Product p
            WHERE p.status = 1
              AND EXISTS (
                SELECT 1
                FROM ProductCategory pc
                WHERE pc.product = p
                  AND pc.category.id = :categoryId
                  AND pc.status = 1
              )
            """)
    Page<Product> findAllByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);


    @Query("""
                SELECT p
                FROM Product p
                WHERE p.status = 1
                  AND p.gender.id = :genderId
            """)
    Page<Product> findAllByGenderId(@Param("genderId") Long genderId, Pageable pageable);

    @Query("""
                SELECT DISTINCT p
                FROM Product p
                JOIN p.productDetails pd
                WHERE p.status = 1
                  AND pd.color.id = :colorId
            """)
    Page<Product> findAllByColorId(@Param("colorId") Long colorId, Pageable pageable);

    @Query("""
                SELECT DISTINCT p
                FROM Product p
                JOIN p.productDetails pd
                WHERE p.status = 1
                  AND pd.size.id = :sizeId
            """)
    Page<Product> findAllBySizeId(@Param("sizeId") Long sizeId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "material","brand","style","gender","sole","supplier",
            "productDetails","productDetails.color","productDetails.size",
            "productImages","productImages.color","productCategories","productCategories.category"
    })
    @Query("""
      SELECT DISTINCT p
      FROM Product p
      WHERE COALESCE(p.status, 1) = 1
        AND (:brandId   IS NULL OR p.brand.id   = :brandId)
        AND (:genderId  IS NULL OR p.gender.id  = :genderId)
        AND (
              :categoryId IS NULL OR EXISTS (
                  SELECT 1 FROM ProductCategory pc
                  WHERE pc.product = p
                    AND COALESCE(pc.status, 1) = 1
                    AND pc.category.id = :categoryId
              )
        )
        AND EXISTS (
              SELECT 1 FROM ProductDetail pd
              WHERE pd.product = p
                AND COALESCE(pd.status, 1) = 1
                AND (:colorId IS NULL OR pd.color.id = :colorId)
                AND (:sizeId  IS NULL OR pd.size.id  = :sizeId)
        )
      ORDER BY p.createdDate DESC
  """)
    Page<Product> searchFull(@Param("brandId") Long brandId,
                             @Param("categoryId") Long categoryId,
                             @Param("genderId") Long genderId,
                             @Param("colorId") Long colorId,
                             @Param("sizeId") Long sizeId,
                             Pageable pageable);

    @Query("""
            select p from Product p left join fetch ProductDetail pd where  pd.productDetailCode = :code
            """)
    Optional<Product> findByProductCode(@Param("code") String code);

    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdProduct(@Param("id") Long id);

    @Query(
            value = """
        SELECT p.*
        FROM product p
        WHERE p.status = 1
          AND (
                :kw IS NULL OR :kw = ''
                OR p.product_name COLLATE Vietnamese_100_CI_AI LIKE '%' + :kw + '%'
                OR p.product_code COLLATE Vietnamese_100_CI_AI LIKE '%' + :kw + '%'
              )
        ORDER BY p.id DESC
        """,
            countQuery = """
        SELECT COUNT(1)
        FROM product p
        WHERE p.status = 1
          AND (
                :kw IS NULL OR :kw = ''
                OR p.product_name COLLATE Vietnamese_100_CI_AI LIKE '%' + :kw + '%'
                OR p.product_code COLLATE Vietnamese_100_CI_AI LIKE '%' + :kw + '%'
              )
        """,
            nativeQuery = true
    )
    Page<Product> searchByKeyword(@Param("kw") String keyword, Pageable pageable);


}
