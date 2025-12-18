package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.dto.response.*;
import com.example.duantotnghiep.model.Customer;
import com.example.duantotnghiep.model.Invoice;
import com.example.duantotnghiep.state.TrangThaiChiTiet;
import com.example.duantotnghiep.state.TrangThaiTong;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("""
SELECT new com.example.duantotnghiep.dto.response.InvoiceResponse(
    i.id,
    i.invoiceCode,
    i.statusDetail,
    i.orderType,
    i.createdDate,
    i.customer.customerName,
    i.phone,
    i.phoneSender,
    i.totalAmount,
    i.finalAmount,

    (SELECT COUNT(i2.id) FROM Invoice i2
     WHERE i2.statusDetail = :statusChoXacNhan
       AND (:createdFrom IS NULL OR i2.createdDate >= :createdFrom)
       AND (:createdTo   IS NULL OR i2.createdDate <= :createdTo)
       AND (:isPaid IS NULL OR i2.isPaid = :isPaid)
       AND (:orderType IS NULL OR i2.orderType = :orderType)
       AND (:phone IS NULL OR i2.phoneSender LIKE CONCAT('%', :phone, '%'))
       AND (:code  IS NULL OR i2.invoiceCode LIKE CONCAT('%', :code, '%'))),

    (SELECT COUNT(i2.id) FROM Invoice i2
     WHERE i2.statusDetail = :statusDaXacNhan
       AND (:createdFrom IS NULL OR i2.createdDate >= :createdFrom)
       AND (:createdTo   IS NULL OR i2.createdDate <= :createdTo)
       AND (:isPaid IS NULL OR i2.isPaid = :isPaid)
       AND (:orderType IS NULL OR i2.orderType = :orderType)
       AND (:phone IS NULL OR i2.phoneSender LIKE CONCAT('%', :phone, '%'))
       AND (:code  IS NULL OR i2.invoiceCode LIKE CONCAT('%', :code, '%'))),

    (SELECT COUNT(i2.id) FROM Invoice i2
     WHERE i2.statusDetail = :statusChoGiaoHang
       AND (:createdFrom IS NULL OR i2.createdDate >= :createdFrom)
       AND (:createdTo   IS NULL OR i2.createdDate <= :createdTo)
       AND (:isPaid IS NULL OR i2.isPaid = :isPaid)
       AND (:orderType IS NULL OR i2.orderType = :orderType)
       AND (:phone IS NULL OR i2.phoneSender LIKE CONCAT('%', :phone, '%'))
       AND (:code  IS NULL OR i2.invoiceCode LIKE CONCAT('%', :code, '%'))),

    (SELECT COUNT(i2.id) FROM Invoice i2
     WHERE i2.statusDetail = :statusDangGiaoHang
       AND (:createdFrom IS NULL OR i2.createdDate >= :createdFrom)
       AND (:createdTo   IS NULL OR i2.createdDate <= :createdTo)
       AND (:isPaid IS NULL OR i2.isPaid = :isPaid)
       AND (:orderType IS NULL OR i2.orderType = :orderType)
       AND (:phone IS NULL OR i2.phoneSender LIKE CONCAT('%', :phone, '%'))
       AND (:code  IS NULL OR i2.invoiceCode LIKE CONCAT('%', :code, '%'))),

    (SELECT COUNT(i2.id) FROM Invoice i2
     WHERE i2.statusDetail = :statusGiaoThanhCong
       AND (:createdFrom IS NULL OR i2.createdDate >= :createdFrom)
       AND (:createdTo   IS NULL OR i2.createdDate <= :createdTo)
       AND (:isPaid IS NULL OR i2.isPaid = :isPaid)
       AND (:orderType IS NULL OR i2.orderType = :orderType)
       AND (:phone IS NULL OR i2.phoneSender LIKE CONCAT('%', :phone, '%'))
       AND (:code  IS NULL OR i2.invoiceCode LIKE CONCAT('%', :code, '%'))),

    (SELECT COUNT(i2.id) FROM Invoice i2
     WHERE i2.statusDetail = :statusGiaoThatBai
       AND (:createdFrom IS NULL OR i2.createdDate >= :createdFrom)
       AND (:createdTo   IS NULL OR i2.createdDate <= :createdTo)
       AND (:isPaid IS NULL OR i2.isPaid = :isPaid)
       AND (:orderType IS NULL OR i2.orderType = :orderType)
       AND (:phone IS NULL OR i2.phoneSender LIKE CONCAT('%', :phone, '%'))
       AND (:code  IS NULL OR i2.invoiceCode LIKE CONCAT('%', :code, '%'))),

    (SELECT COUNT(i2.id) FROM Invoice i2
     WHERE i2.statusDetail = :statusHuyDon
       AND (:createdFrom IS NULL OR i2.createdDate >= :createdFrom)
       AND (:createdTo   IS NULL OR i2.createdDate <= :createdTo)
       AND (:isPaid IS NULL OR i2.isPaid = :isPaid)
       AND (:orderType IS NULL OR i2.orderType = :orderType)
       AND (:phone IS NULL OR i2.phoneSender LIKE CONCAT('%', :phone, '%'))
       AND (:code  IS NULL OR i2.invoiceCode LIKE CONCAT('%', :code, '%')))
)
FROM Invoice i
WHERE (:status IS NULL OR i.statusDetail = :status)
  AND (:isPaid IS NULL OR i.isPaid = :isPaid)
  AND (:orderType IS NULL OR i.orderType = :orderType)
  AND (:createdFrom IS NULL OR i.createdDate >= :createdFrom)
  AND (:createdTo   IS NULL OR i.createdDate <= :createdTo)
  AND (:phone IS NULL OR i.phoneSender LIKE CONCAT('%', :phone, '%'))
  AND (:code  IS NULL OR i.invoiceCode LIKE CONCAT('%', :code, '%'))
ORDER BY i.createdDate ASC
""")
    List<InvoiceResponse> searchInvoices(
            @Param("status") TrangThaiChiTiet status,
            @Param("isPaid") Boolean isPaid,
            @Param("orderType") Integer orderType,
            @Param("createdFrom") Date createdFrom,
            @Param("createdTo") Date createdTo,
            @Param("phone") String phone,
            @Param("code") String code,
            @Param("statusChoXacNhan") TrangThaiChiTiet statusChoXacNhan,
            @Param("statusDaXacNhan") TrangThaiChiTiet statusDaXacNhan,
            @Param("statusChoGiaoHang") TrangThaiChiTiet statusChoGiaoHang,
            @Param("statusDangGiaoHang") TrangThaiChiTiet statusDangGiaoHang,
            @Param("statusGiaoThanhCong") TrangThaiChiTiet statusGiaoThanhCong,
            @Param("statusGiaoThatBai") TrangThaiChiTiet statusGiaoThatBai,
            @Param("statusHuyDon") TrangThaiChiTiet statusHuyDon
    );



    List<Invoice> findByStatus(int status);

    @Query(
            "SELECT i FROM Invoice i LEFT JOIN i.customer c " +
                    "WHERE (" +
                    "  :keyword IS NULL OR :keyword = '' OR " +
                    "  LOWER(i.invoiceCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "  LOWER(COALESCE(c.customerName, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "  LOWER(COALESCE(c.phone, ''))       LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "  LOWER(COALESCE(i.phone, ''))       LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    ") " +
                    "AND (:startOfDay IS NULL OR i.createdDate >= :startOfDay) " +
                    "AND (:startOfNextDay IS NULL OR i.createdDate < :startOfNextDay) " +
                    "AND (" +
                    // 1) Nếu CHỌN trạng thái TẠI QUẦY -> chỉ lấy orderType=0 và map 3 trạng thái
                    "  ( :counterStatusKey IS NOT NULL AND :counterStatusKey <> '' AND " +
                    "    i.orderType = 0 AND (" +
                    "      (:counterStatusKey = 'DANG_XU_LY' AND i.status = com.example.duantotnghiep.state.TrangThaiTong.DANG_XU_LY) OR " +
                    "      (:counterStatusKey = 'THANH_CONG' AND i.status = com.example.duantotnghiep.state.TrangThaiTong.THANH_CONG) OR " +
                    "      (:counterStatusKey = 'DA_HUY'     AND i.status = com.example.duantotnghiep.state.TrangThaiTong.DA_HUY) " +
                    "    ) " +
                    "  ) " +

                    // 2) Nếu KHÔNG chọn tại quầy và KHÔNG chọn online -> không lọc theo trạng thái (trả cả 0/1)
                    "  OR ( ( :counterStatusKey IS NULL OR :counterStatusKey = '' ) " +
                    "       AND ( :onlineStatusKey  IS NULL OR :onlineStatusKey  = '' ) ) " +

                    // 3) Nếu KHÔNG chọn tại quầy nhưng CHỌN online -> chỉ lấy orderType=1 và map trạng thái chi tiết
                    "  OR ( ( :counterStatusKey IS NULL OR :counterStatusKey = '' ) AND " +
                    "       :onlineStatusKey IS NOT NULL AND :onlineStatusKey <> '' AND " +
                    "       i.orderType = 1 AND (" +
                    "         (:onlineStatusKey = 'CHO_XU_LY'       AND i.statusDetail = com.example.duantotnghiep.state.TrangThaiChiTiet.CHO_XU_LY) OR " +
                    "         (:onlineStatusKey = 'DA_XU_LY'        AND i.statusDetail = com.example.duantotnghiep.state.TrangThaiChiTiet.DA_XU_LY) OR " +
                    "         (:onlineStatusKey = 'CHO_GIAO_HANG'   AND i.statusDetail = com.example.duantotnghiep.state.TrangThaiChiTiet.CHO_GIAO_HANG) OR " +
                    "         (:onlineStatusKey = 'DANG_GIAO_HANG'  AND i.statusDetail = com.example.duantotnghiep.state.TrangThaiChiTiet.DANG_GIAO_HANG) OR " +
                    "         (:onlineStatusKey = 'GIAO_THANH_CONG' AND i.statusDetail = com.example.duantotnghiep.state.TrangThaiChiTiet.GIAO_THANH_CONG) OR " +
                    "         (:onlineStatusKey = 'GIAO_THAT_BAI'   AND i.statusDetail = com.example.duantotnghiep.state.TrangThaiChiTiet.GIAO_THAT_BAI) OR " +
                    "         (:onlineStatusKey = 'HUY_DON'         AND i.statusDetail = com.example.duantotnghiep.state.TrangThaiChiTiet.HUY_DON) " +
                    "       ) " +
                    "  ) " +
                    ") " +
                    "ORDER BY i.createdDate DESC"
    )
    Page<Invoice> searchByKeywordStatusSeparatedAndCreatedDate(
            @Param("keyword") String keyword,
            @Param("counterStatusKey") String counterStatusKey,
            @Param("onlineStatusKey") String onlineStatusKey,
            @Param("startOfDay") java.util.Date startOfDay,
            @Param("startOfNextDay") java.util.Date startOfNextDay,
            Pageable pageable
    );

    long countByStatusAndOrderTypeAndEmployee_Id(TrangThaiTong status, Integer orderType, Long employeeId);


    Invoice findByInvoiceCode(String invoiceCode);

    @Query("SELECT i FROM Invoice i WHERE i.invoiceCode = :invoiceCode")
    Optional<Invoice> findByInvoiceCodeQR(@Param("invoiceCode") String invoiceCode);

    Page<Invoice> findByStatusAndEmployeeIdAndOrderType(
            TrangThaiTong status, Long employeeId, int orderType, Pageable pageable);

    List<Invoice> findByStatusAndOrderTypeAndCreatedDateBefore(TrangThaiTong status, int orderType, LocalDateTime time);

    boolean existsByCustomer_IdAndStatusAndOrderType(Long customerId, TrangThaiTong status, Integer orderType);

    @Query("SELECT i FROM Invoice i WHERE i.id = :id AND i.isPaid = :isPaid")
    Optional<Invoice> findPaidInvoiceById(@Param("id") Long id, @Param("isPaid") Boolean isPaid);


    @Query("""
                SELECT MONTH(i.createdDate), SUM(i.finalAmount)
                FROM Invoice i
                WHERE YEAR(i.createdDate) = :year AND i.status = :status
                GROUP BY MONTH(i.createdDate)
                ORDER BY MONTH(i.createdDate)
            """)
    List<Object[]> getMonthlyRevenue(@Param("year") int year, @Param("status") TrangThaiTong status);

    @Query("""
                SELECT YEAR(i.createdDate), SUM(i.finalAmount)
                FROM Invoice i
                WHERE i.status = :status
                GROUP BY YEAR(i.createdDate)
                ORDER BY YEAR(i.createdDate)
            """)
    List<Object[]> getYearlyRevenue(@Param("status") TrangThaiTong status);

    @Query("""
                SELECT i.orderType, SUM(i.finalAmount)
                FROM Invoice i
                WHERE i.status = :status
                GROUP BY i.orderType
            """)
    List<Object[]> getRevenueByOrderType(@Param("status") TrangThaiTong status);

    @Query("""
                SELECT pd.product.id, pd.product.productName, SUM(idt.quantity)
                FROM InvoiceDetail idt
                JOIN idt.invoice i
                JOIN idt.productDetail pd
                WHERE i.status = :status AND i.createdDate BETWEEN :start AND :end
                GROUP BY pd.product.id, pd.product.productName
                ORDER BY SUM(idt.quantity) DESC
            """)
    List<Object[]> getTopSellingProducts(@Param("status") TrangThaiTong status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("""
                SELECT i.status, COUNT(i)
                FROM Invoice i
                GROUP BY i.status
            """)
    List<Object[]> countInvoicesByStatus();

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i " +
            "WHERE i.createdDate >= :startOfDay AND i.createdDate < :endOfDay AND i.status = :status")
    Long getTodayRevenue(@Param("startOfDay") LocalDateTime startOfDay,
                         @Param("endOfDay") LocalDateTime endOfDay,
                         @Param("status") TrangThaiTong status);

    Optional<Invoice> findByAppTransId(String appTransId);

    @Query("SELECT i.statusDetail, COUNT(i) FROM Invoice i WHERE i.statusDetail IS NOT NULL GROUP BY i.statusDetail")
    List<Object[]> countByStatusDetail();

    @Query("SELECT i FROM Invoice i WHERE i.customer.id = :customerId AND i.status = :status AND i.orderType = 1")
    List<Invoice> findSuccessfulOnlineInvoicesByCustomerId(@Param("customerId") Long customerId, @Param("status") TrangThaiTong status);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.customer.id = :customerId AND i.status = :paidStatus")
    Long countPaidInvoicesByCustomer(@Param("customerId") Long customerId,
                                     @Param("paidStatus") TrangThaiTong paidStatus);

//    @Query("""
//    select new com.example.duantotnghiep.dto.response.HuyDonResponse(
//        i.id,
//        count(i.id),
//        sum(id.quantity),
//        sum(i.finalAmount)
//    )
//    from Invoice i
//    left join InvoiceDetail id on id.invoice.id = i.id
//    where i.employee.id = :employeeId and i.status = 'CANCELLED'
//    group by i.id
//""")
//    List<HuyDonResponse> getHuyDonEmployee(@Param("employeeId") Long employeeId);

    int countByCustomerAndStatusDetailAndUpdatedDateAfter(
            Customer customer,
            TrangThaiChiTiet statusDetail,
            LocalDateTime updatedAfter
    );

    @Query("""
                SELECT new com.example.duantotnghiep.dto.response.DiscountCampaignStatisticsResponse(
                    COUNT(DISTINCT i.id),
                    SUM(COALESCE(id.sellPrice, 0) * COALESCE(id.quantity, 0)),
                    SUM(COALESCE(id.discountedPrice, 0) * COALESCE(id.quantity, 0)),
                    SUM(COALESCE(id.quantity, 0)),
                    CASE 
                        WHEN SUM(COALESCE(id.sellPrice, 0) * COALESCE(id.quantity, 0)) = 0 THEN 0
                        ELSE 
                            (100.0 * 
                                (SUM(COALESCE(id.sellPrice, 0) * COALESCE(id.quantity, 0)) 
                                 - SUM(COALESCE(id.discountedPrice, 0) * COALESCE(id.quantity, 0)))
                            / SUM(COALESCE(id.sellPrice, 0) * COALESCE(id.quantity, 0))
                            )
                    END
                )
                FROM InvoiceDetail id
                JOIN id.invoice i
                WHERE id.discountCampaign.id = :campaignId 
                  AND id.status = 1
            """)
    DiscountCampaignStatisticsResponse getStatisticsByCampaignId(@Param("campaignId") Long campaignId);

    @Query(value = """
            SELECT
                e.id,
                e.employee_name AS employeeName,
                COUNT(DISTINCT i.id) AS totalInvoices,
                SUM(idt.quantity) AS totalProducts,
                SUM(i.final_amount) AS totalRevenue,
                COUNT(DISTINCT CASE WHEN i.status = 1 THEN i.id END) AS successInvoices,
                SUM(CASE WHEN i.status = 1 THEN idt.quantity ELSE 0 END) AS successProducts,
                SUM(CASE WHEN i.status = 1 THEN i.final_amount ELSE 0 END) AS successRevenue,
                COUNT(DISTINCT CASE WHEN i.status = 2 THEN i.id END) AS cancelledInvoices,
                SUM(CASE WHEN i.status = 2 THEN idt.quantity ELSE 0 END) AS cancelledProducts,
                SUM(CASE WHEN i.status = 2 THEN i.final_amount ELSE 0 END) AS cancelledRevenue
            FROM invoice i
            JOIN employee e ON i.employee_id = e.id
            JOIN invoice_details idt ON i.id = idt.invoice_id
            WHERE (:employeeId IS NULL OR e.id = :employeeId)
              AND (:startDate IS NULL OR i.created_date >= :startDate)
              AND (:endDate IS NULL OR i.created_date <= :endDate)
            GROUP BY e.id, e.employee_name
            ORDER BY e.employee_name
            """, nativeQuery = true)
    List<Object[]> getEmployeeSalesReportNative(
            @Param("employeeId") Long employeeId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    @Query(value = """
        SELECT
            CASE status_detail
                WHEN 0 THEN 'CHO_XU_LY'
                WHEN 1 THEN 'DA_XU_LY'
                WHEN 2 THEN 'CHO_GIAO_HANG'
                WHEN 3 THEN 'DANG_GIAO_HANG'
                WHEN 4 THEN 'GIAO_THANH_CONG'
                WHEN 5 THEN 'GIAO_THAT_BAI'
                WHEN 9 THEN 'YEU_CAU_HUY'
                WHEN -2 THEN 'HUY_DON'
            END AS statusDetail,
            COUNT(*) AS countInvoice
        FROM invoice
        WHERE order_type = 1
          AND customer_id = :customerId
        GROUP BY status_detail
        """, nativeQuery = true)
    List<Object[]> countInvoicesByStatusNative(@Param("customerId") Long customerId);

    @Query(value = """
    SELECT
        CASE status_detail
            WHEN 0 THEN 'CHO_XU_LY'
            WHEN 1 THEN 'DA_XU_LY'
            WHEN 2 THEN 'CHO_GIAO_HANG'
            WHEN 3 THEN 'DANG_GIAO_HANG'
            WHEN 4 THEN 'GIAO_THANH_CONG'
            WHEN 5 THEN 'GIAO_THAT_BAI'
            WHEN 9 THEN 'YEU_CAU_HOAN'
            WHEN -2 THEN 'HUY_DON'
        END AS statusDetail,
        COUNT(*) AS countInvoice
    FROM invoice
    WHERE order_type = 1
    GROUP BY status_detail
    ORDER BY status_detail
    """, nativeQuery = true)
    List<Object[]> countInvoicesByStatusNativeV2();



    int countByCustomerAndStatusDetailAndUpdatedDateAfter(
            Customer customer,
            TrangThaiChiTiet statusDetail,
            Date updatedAfter
    );

    int countByCustomerAndStatusAndUpdatedDateAfter(
            Customer customer,
            TrangThaiTong status,
            Date updatedAfter
    );

    @Query(
            value = """
                      SELECT SUM(i.final_amount) 
                      FROM invoice i 
                      WHERE i.created_date >= :fromDate 
                        AND i.created_date < :toDate 
                        AND i.status = 1
                    """,
            nativeQuery = true
    )
    BigDecimal sumRevenueBetween(@Param("fromDate") Date fromDate,
                                 @Param("toDate") Date toDate);

    @Query("select i.statusDetail from Invoice i where i.invoiceCode = :code ")
    TrangThaiChiTiet findStatusDetailByCode(String code);

    @Query("""
                SELECT COALESCE(SUM(i.finalAmount), 0)  
                FROM Invoice i
                WHERE i.status IN :successSet
                  AND i.createdDate >= :start AND i.createdDate < :end
            """)
    BigDecimal sumRevenueBetweenSuccessSet(@Param("start") Date start,
                                           @Param("end") Date end,
                                           @Param("successSet") Set<TrangThaiTong> successSet);

    @Query("""
                SELECT i
                FROM Invoice i
                WHERE i.status IN :successSet
                  AND i.createdDate >= :start AND i.createdDate < :end
            """)
    List<Invoice> findSuccessBetweenSet(@Param("start") Date start,
                                        @Param("end") Date end,
                                        @Param("successSet") Set<TrangThaiTong> successSet);

    @Query("""
                SELECT FUNCTION('YEAR', i.createdDate), FUNCTION('MONTH', i.createdDate),
                       COALESCE(SUM(i.finalAmount),0), COUNT(i)
                FROM Invoice i
                WHERE i.status IN :successSet
                  AND i.createdDate >= :start AND i.createdDate < :end
                GROUP BY FUNCTION('YEAR', i.createdDate), FUNCTION('MONTH', i.createdDate)
                ORDER BY FUNCTION('YEAR', i.createdDate), FUNCTION('MONTH', i.createdDate)
            """)
    List<Object[]> aggregateMonthlySet(@Param("start") Date start,
                                       @Param("end") Date end,
                                       @Param("successSet") Set<TrangThaiTong> successSet);

    @Query("""
                SELECT FUNCTION('YEAR', i.createdDate),
                       COALESCE(SUM(i.finalAmount),0), COUNT(i)
                FROM Invoice i
                WHERE i.status IN :successSet
                  AND i.createdDate >= :start AND i.createdDate < :end
                GROUP BY FUNCTION('YEAR', i.createdDate)
                ORDER BY FUNCTION('YEAR', i.createdDate)
            """)
    List<Object[]> aggregateYearlySet(@Param("start") Date start,
                                      @Param("end") Date end,
                                      @Param("successSet") Set<TrangThaiTong> successSet);

    // InvoiceRepository.java
    @Query("""
                SELECT i.status, COUNT(i)
                FROM Invoice i
                WHERE i.createdDate >= :start AND i.createdDate < :end
                GROUP BY i.status
            """)
    List<Object[]> countByStatus(@Param("start") Date start,
                                 @Param("end") Date end);

    @Query("""
        select i from Invoice i
        where i.voucher.id = :voucherId
          and i.status = :status
          and (i.isPaid = false or i.isPaid is null)
    """)
    List<Invoice> findAllByVoucherIdAndStatusAndUnpaid(@Param("voucherId") Long voucherId,
                                                       @Param("status") TrangThaiTong status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i left join fetch i.invoiceDetails d left join fetch d.productDetail pd left join fetch pd.product where i.appTransId = :appTransId")
    Optional<Invoice> findByAppTransIdForUpdate(@Param("appTransId") String appTransId);

    @Query("""
select dc.status
from Invoice i
left join i.invoiceDetails id
left join id.discountCampaign dc
where i.invoiceCode = :code
""")
    List<Integer> getDiscountCampaignStatus(@Param("code") String code);



}


