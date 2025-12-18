package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.dto.response.InvoiceOnlineResponse;
import com.example.duantotnghiep.dto.response.InvoiceResponse;
import com.example.duantotnghiep.state.TrangThaiChiTiet;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;

@Repository
public class InvoiceRepository2 {
    @PersistenceContext
    private EntityManager entityManager;

    private static final String BASE_SELECT = """
        SELECT
            i.id, i.invoice_code, i.customer_id, c.customer_name,
            i.employee_id, e.employee_name,i.phone,i.phone_sender,
            i.total_amount, i.discount_amount, i.final_amount,
            i.description, i.status, i.status_detail,
            i.created_date, i.updated_date,
            i.delivery_address, i.shipping_fee, i.delivered_at,i.is_paid
        FROM invoice i
        LEFT JOIN customer c ON c.id = i.customer_id
        LEFT JOIN employee e ON e.id = i.employee_id
    """;

    public InvoiceOnlineResponse getOrder(Long invoiceId) {
        String sql = BASE_SELECT + " WHERE i.id = :invoiceId";
        try {
            Object[] row = (Object[]) entityManager.createNativeQuery(sql)
                    .setParameter("invoiceId", invoiceId)
                    .getSingleResult();
            return mapRowToInvoice(row);
        } catch (NoResultException e) {
            throw new RuntimeException("Không tìm thấy đơn hàng với ID: " + invoiceId);
        }
    }

    public InvoiceOnlineResponse getOrder2(Long invoiceId, Long customerId) {
        String sql = BASE_SELECT + " WHERE i.id = :invoiceId AND i.customer_id = :customerId";
        try {
            Object[] row = (Object[]) entityManager.createNativeQuery(sql)
                    .setParameter("invoiceId", invoiceId)
                    .setParameter("customerId", customerId)
                    .getSingleResult();
            return mapRowToInvoice(row);
        } catch (NoResultException e) {
            throw new RuntimeException("Không tìm thấy đơn hàng với ID: " + invoiceId + " và customerId: " + customerId);
        }
    }

    public List<InvoiceOnlineResponse> getOrder3(Long customerId,Integer statusDetail) {
        String sql = BASE_SELECT + " WHERE i.customer_id = :customerId and i.status_detail = :statusDetail";
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("customerId", customerId)
                .setParameter("statusDetail",statusDetail)
                .getResultList();

        return rows.stream()
                .map(this::mapRowToInvoice)
                .toList();
    }

    public List<InvoiceOnlineResponse> getOrder4(Long customerId,Integer statusDetail) {
        String sql = BASE_SELECT + " WHERE i.customer_id = :customerId and i.status_detail = :statusDetail and i.order_type = 1 ";
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("customerId", customerId)
                .setParameter("statusDetail",statusDetail)
                .getResultList();

        return rows.stream()
                .map(this::mapRowToInvoice)
                .toList();
    }

    // Mapping helper
    private InvoiceOnlineResponse mapRowToInvoice(Object[] row) {
        if (row == null || row.length < 17) {
            throw new RuntimeException("Dữ liệu không đầy đủ. Số cột nhận được: " + (row != null ? row.length : 0));
        }

        return new InvoiceOnlineResponse(
                toLong(row[0]),    // id
                toString(row[1]),  // invoice_code
                toLong(row[2]),    // customer_id
                toString(row[3]),  // customer_name
                toLong(row[4]),    // employee_id
                toString(row[5]),  // employee_name
                toString(row[6]),  // phone
                toString(row[7]),
                toBigDecimal(row[8]),   // total_amount
                toBigDecimal(row[9]),   // discount_amount
                toBigDecimal(row[10]),   // final_amount
                toString(row[11]),      // description
                toInt(row[12]),         // status
                toInt(row[13]),         // status_detail
                toDate(row[14]),        // created_date
                toDate(row[15]),        // updated_date
                toString(row[16]),      // delivery_address
                toBigDecimal(row[17]),  // shipping_fee
                toDate(row[18]),        // delivered_at
                toBoolean(row[19])      // ...
        );
    }


    @SuppressWarnings("unchecked")
    public List<Object[]> findDeliveredRows(Long customerId,
                                            Integer onlyUnrated,   // 0/1
                                            String keyword,        // có thể null
                                            Date dateFrom,         // có thể null
                                            Date dateTo) {         // có thể null
        String sql = """
            SELECT
                -- Hóa đơn -> FavouriteResponse
                i.id                                AS invoiceId,
                i.invoice_code                      AS invoiceCode,
                i.delivered_at                      AS deliveredAt,
                i.final_amount                      AS finalAmount,
                c.customer_name                     AS customerName,  
                idt.id                              AS id,     
                p.id                                AS productId,
                p.product_name                      AS productName,
                p.product_code                      AS productCode,
                sz.size_name                        AS sizeName,
                cl.color_name                       AS colorName,
                idt.sell_price                      AS sellPrice,
                idt.discount_percentage             AS discountPercentage,
                (idt.sell_price - ISNULL(idt.discounted_price, idt.sell_price)) AS discountAmount,
                idt.discounted_price                AS discountedPrice,
                idt.quantity                        AS quantity,
                idt.invoice_detail_code             AS invoiceCodeDetail,
                (idt.sell_price * idt.quantity)     AS totalPrice,
                (ISNULL(idt.discounted_price, idt.sell_price) * idt.quantity) AS finalTotalPrice,
                fp.rate                             AS rate,
                fp.comment                          AS comment,
                CASE WHEN fp.rate IS NULL THEN CAST(0 AS bit) ELSE CAST(1 AS bit) END AS rated,
                img.image                           AS image       
            
            FROM invoice i
            JOIN invoice_details    idt ON idt.invoice_id = i.id
            JOIN product_details    pd  ON pd.id = idt.product_detail_id
            JOIN product            p   ON p.id = pd.product_id
            LEFT JOIN size          sz  ON sz.id = pd.size_id       
            LEFT JOIN color         cl  ON cl.id = pd.color_id        
            LEFT JOIN Customer      c   ON c.id = i.customer_id
            
            OUTER APPLY (
                SELECT TOP 1 pi.image
                FROM product_image pi
                WHERE pi.product_id = p.id
                  AND (pi.color_id = pd.color_id OR pi.color_id IS NULL)
                  AND pi.status = 1
                ORDER BY pi.id ASC
            ) AS img
            
            LEFT JOIN favorite_product fp
                   ON fp.product_id  = p.id
                  AND fp.customer_id = i.customer_id
                  AND (fp.invoice_id = i.id OR fp.invoice_id IS NULL)
            
            WHERE
                i.customer_id   = :customerId
                AND i.status_detail = 4  
            
                AND (:onlyUnrated = 0 OR (:onlyUnrated = 1 AND fp.rate IS NULL))
            
                AND (:kw IS NULL
                     OR i.invoice_code LIKE :kw
                     OR p.product_name LIKE :kw)
            
                AND (:dateFrom IS NULL OR i.delivered_at >= :dateFrom)
                AND (:dateTo   IS NULL OR i.delivered_at <  DATEADD(day, 1, :dateTo))
            
            ORDER BY i.delivered_at DESC, i.id DESC, idt.id ASC
            
        """;

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("customerId", customerId);
        q.setParameter("onlyUnrated", onlyUnrated == null ? 0 : onlyUnrated);
        q.setParameter("kw", (keyword == null || keyword.isBlank()) ? null : "%" + keyword.trim() + "%");
        q.setParameter("dateFrom", dateFrom);
        q.setParameter("dateTo", dateTo);

        return (List<Object[]>) q.getResultList();
    }

    private Long toLong(Object o) {
        return o != null ? ((Number) o).longValue() : null;
    }

    private int toInt(Object o) {
        return o != null ? ((Number) o).intValue() : 0; //  tránh NPE bằng cách trả về 0 nếu null
    }

    private BigDecimal toBigDecimal(Object o) {
        return o != null ? (BigDecimal) o : BigDecimal.ZERO; // optional: có thể là null nếu bạn muốn
    }

    private String toString(Object o) {
        return o != null ? o.toString() : null;
    }

    private Date toDate(Object o) {
        return o != null ? (Date) o : null;
    }
    private Boolean toBoolean(Object o) {
        return o != null ? Boolean.valueOf(o.toString()) : false;
    }



}
