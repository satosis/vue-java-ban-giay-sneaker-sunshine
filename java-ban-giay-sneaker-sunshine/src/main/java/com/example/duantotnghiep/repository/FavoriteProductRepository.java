package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.dto.response.FavoriteProductResponse;
import com.example.duantotnghiep.dto.response.TopRatedProductDTO;
import com.example.duantotnghiep.model.FavoriteProduct;
import com.example.duantotnghiep.model.ProductRatingView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteProductRepository extends JpaRepository<FavoriteProduct, Long> {
    Optional<FavoriteProduct> findByCustomerIdAndProductIdAndStatus(Long customerId, Long productId, int status);

    @Query("""
                SELECT f FROM FavoriteProduct f
                WHERE f.customer.id = :customerId AND f.status = 1
            """)
    List<FavoriteProduct> getFavoritesByCustomer(@Param("customerId") Long customerId);

    @Query("""
                select 
                    fp.product.id as productId,
                    avg(cast(fp.rate as double)) as avgRating,
                    count(fp.id) as totalReviews,
                    sum(case when fp.rate = 1 then 1 else 0 end) as star1,
                    sum(case when fp.rate = 2 then 1 else 0 end) as star2,
                    sum(case when fp.rate = 3 then 1 else 0 end) as star3,
                    sum(case when fp.rate = 4 then 1 else 0 end) as star4,
                    sum(case when fp.rate = 5 then 1 else 0 end) as star5
                from FavoriteProduct fp
                where fp.status = 1 
                  and fp.rate is not null
                  and fp.product.id = :productId
                group by fp.product.id
            """)
    Optional<ProductRatingView> findRatingByProductId(@Param("productId") Long productId);

    // Nhiều sản phẩm (bulk) -> trả list, mỗi phần tử là 1 productId
    @Query("""
                select 
                    fp.product.id as productId,
                    avg(cast(fp.rate as double)) as avgRating,
                    count(fp.id) as totalReviews,
                    sum(case when fp.rate = 1 then 1 else 0 end) as star1,
                    sum(case when fp.rate = 2 then 1 else 0 end) as star2,
                    sum(case when fp.rate = 3 then 1 else 0 end) as star3,
                    sum(case when fp.rate = 4 then 1 else 0 end) as star4,
                    sum(case when fp.rate = 5 then 1 else 0 end) as star5
                from FavoriteProduct fp
                where fp.status = 1
                  and fp.rate is not null
                  and fp.product.id in :productIds
                group by fp.product.id
            """)
    List<ProductRatingView> findRatingsByProductIds(@Param("productIds") List<Long> productIds);

    @Query("""
            select new com.example.duantotnghiep.dto.response.FavoriteProductResponse(
            fp.id,fp.customer.id,c.customerName,p.id,fp.rate,fp.comment,fp.createdAt
            ) from FavoriteProduct fp 
              left join Product p on p.id = fp.product.id 
              left join Customer c on c.id = fp.customer.id
              where p.id = :productId and fp.status = 1 order by fp.createdAt desc 
            """)
    List<FavoriteProductResponse> getFavoritesByProductId(@Param("productId") Long productId);

    @Query("""
select new com.example.duantotnghiep.dto.response.TopRatedProductDTO(
    p.id,
    p.productName,
    p.productCode,

    coalesce((
        select avg(1.0 * fp2.rate)
        from FavoriteProduct fp2
        where fp2.product = p and fp2.rate is not null
    ), 0.0),

    (select count(fp3.id)
     from FavoriteProduct fp3
     where fp3.product = p and fp3.rate is not null),

    (select count(fp4.id)
     from FavoriteProduct fp4
     where fp4.product = p and fp4.status = 1),

    coalesce((
        select sum(idt2.quantity)
        from InvoiceDetail idt2
        join idt2.productDetail pd2
        join idt2.invoice inv2
        where pd2.product = p
          and inv2.status in (
              com.example.duantotnghiep.state.TrangThaiTong.DANG_XU_LY,
              com.example.duantotnghiep.state.TrangThaiTong.THANH_CONG
          )
    ), 0L)
)
from Product p
where (select count(fp5.id)
       from FavoriteProduct fp5
       where fp5.product = p and fp5.rate is not null) >= :minReviews
order by
    coalesce((
        select avg(1.0 * fp6.rate)
        from FavoriteProduct fp6
        where fp6.product = p and fp6.rate is not null
    ), 0.0) desc,

    (select count(fp7.id)
     from FavoriteProduct fp7
     where fp7.product = p and fp7.rate is not null) desc,

    (select count(fp8.id)
     from FavoriteProduct fp8
     where fp8.product = p and fp8.status = 1) desc,

    p.id asc
""")
    List<com.example.duantotnghiep.dto.response.TopRatedProductDTO> findTopRatedWithSalesAndFavCount(
            org.springframework.data.domain.Pageable pageable,
            @org.springframework.data.repository.query.Param("minReviews") long minReviews
    );


}
