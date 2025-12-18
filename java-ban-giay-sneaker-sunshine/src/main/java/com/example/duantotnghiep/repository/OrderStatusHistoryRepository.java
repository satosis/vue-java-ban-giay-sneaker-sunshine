package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.dto.response.OrderStatusHistoryResponse;
import com.example.duantotnghiep.model.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory,Long> {

    @Query("""
    select new com.example.duantotnghiep.dto.response.OrderStatusHistoryResponse(
        o.id, o.invoice.id, o.employee.id, o.employee.employeeName,
        o.oldStatus, o.newStatus, o.changedAt, o.note
    )
    from OrderStatusHistory o
    where o.invoice.id = :invoiceId
    order by o.changedAt desc
""")
    List<OrderStatusHistoryResponse> getOrderStatusHistoriesByInvoice(@Param("invoiceId") Long invoiceId);

    @Query("""
    SELECT COUNT(h) > 0 
    FROM OrderStatusHistory h 
    WHERE h.invoice.id = :invoiceId 
      AND h.employee.id <> :employeeId 
""")
    Boolean isProcessedByAnotherEmployee(@Param("invoiceId") Long invoiceId,
                                         @Param("employeeId") Long employeeId);



}
