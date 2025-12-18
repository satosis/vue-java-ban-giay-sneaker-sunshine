package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.dto.response.InvoiceDetailOnline;
import com.example.duantotnghiep.dto.response.RatingProductResponse;
import com.example.duantotnghiep.model.Invoice;
import com.example.duantotnghiep.model.InvoiceDetail;
import com.example.duantotnghiep.model.ProductDetail;
import com.example.duantotnghiep.state.TrangThaiTong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface InvoiceDetailRepository extends JpaRepository<InvoiceDetail, Long> {
    List<InvoiceDetail> findByInvoiceId(Long invoiceId);

    @Query("SELECT d FROM InvoiceDetail d WHERE d.invoice.invoiceCode = :invoiceCode")
    List<InvoiceDetail> findByInvoiceCodeQR(@Param("invoiceCode") String invoiceCode);


    Optional<InvoiceDetail> findByInvoiceAndProductDetail(Invoice invoice, ProductDetail productDetail);

    List<InvoiceDetail> findByInvoiceAndStatus(Invoice invoice, Integer status);

    @Query("""
                select id from InvoiceDetail id
                join id.productDetail pd
                where id.invoice = :invoice
                  and id.status = :status
                  and pd.status = 1
            """)
    List<InvoiceDetail> findByInvoiceAndStatusAndProductDetailActive(
            @Param("invoice") Invoice invoice,
            @Param("status") Integer status
    );


    List<InvoiceDetail> findByInvoiceIdIn(Collection<Long> invoiceIds);


    @Query(value = """
                SELECT 
                    i.customer_id AS customerId,
                    p.id AS productId,
                    p.product_name AS productName,
                    (
                        SELECT TOP 1 pi.image 
                        FROM product_image pi 
                        WHERE pi.product_id = p.id
                    ) AS image, 
                    CASE
                        WHEN EXISTS (
                            SELECT 1 
                            FROM favorite_product r
                            WHERE r.customer_id = :customerId 
                              AND r.product_id = p.id
                        ) THEN 1
                        ELSE 0
                    END AS isRated
                FROM invoice i
                LEFT JOIN invoice_details id ON id.invoice_id = i.id
                LEFT JOIN product_details pd ON pd.id = id.product_detail_id
                LEFT JOIN product p ON p.id = pd.product_id
                WHERE i.customer_id = :customerId and i.status = 1
                GROUP BY i.customer_id, p.id, p.product_name
            """, nativeQuery = true)
    List<Object[]> findAllByCustomerId(@Param("customerId") Long customerId);

    @Query("""
                select new com.example.duantotnghiep.dto.response.InvoiceDetailOnline(
                id.id,id.invoiceCodeDetail,id.invoice.id,id.productDetail.id,id.productDetail.product.productName,id.productDetail.size.id,id.productDetail.size.sizeName,
                id.productDetail.color.id,id.productDetail.color.colorName,id.quantity,id.sellPrice,id.discountedPrice,id.discountPercentage
                ) from InvoiceDetail  id
                where id.invoice.id =:invoiceId
            """)
    List<InvoiceDetailOnline> findByInvoiceDetailOnline(@Param("invoiceId") Long invoiceId);

    @Query("""
                select coalesce(sum(d.quantity), 0)
                from InvoiceDetail d
                join d.invoice inv
                where inv.status = 1
                  and d.productDetail.product.id = :productId
            """)
    Long countSoldQuantityByProductId(@Param("productId") Long productId);

    @Query("""
        SELECT pd.product.id, pd.product.productName, COALESCE(SUM(d.quantity), 0)
        FROM InvoiceDetail d
        JOIN d.invoice i
        JOIN d.productDetail pd
        WHERE i.status IN :successSet
          AND i.createdDate >= :start AND i.createdDate < :end
        GROUP BY pd.product.id, pd.product.productName
        ORDER BY COALESCE(SUM(d.quantity), 0) DESC
    """)
    List<Object[]> topProducts(@Param("start") Date start,
                               @Param("end") Date end,
                               @Param("successSet") Set<TrangThaiTong> successSet);

    @Query("""
        select id from InvoiceDetail id where id.invoice.id = :invoiceId and id.status = 1
                """)
    List<InvoiceDetail> findByInvoiceIdAndStatus(@Param("invoiceId") Long  invoiceId);

}