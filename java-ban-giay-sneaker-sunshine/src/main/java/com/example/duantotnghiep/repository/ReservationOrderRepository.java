package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.ReservationOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationOrderRepository extends JpaRepository<ReservationOrder, Long> {

    @Query("""
        select coalesce(sum(ro.quantity), 0)
        from ReservationOrder ro
        where ro.productDetailId = :productDetailId and ro.status = 1
    """)
    Integer sumQuantityByProductDetailActive(@Param("productDetailId") Long productDetailId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update ReservationOrder ro
       set ro.status = 0
     where ro.productDetailId = :productDetailId
       and ro.invoiceId = :invoiceId
       and ro.status = 1
""")
    int deactivateByProductDetailAndInvoice(@Param("productDetailId") Long productDetailId,
                                            @Param("invoiceId") Long invoiceId);

    @Query("""
           select ro from ReservationOrder ro where ro.status = 1 and ro.invoiceId = :invoiceId
        """)
    List<ReservationOrder> findByInvoiceId(@Param("invoiceId") Long invoiceId);

}
