package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.dto.response.ProductHistoryResponse;
import com.example.duantotnghiep.model.ProductHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductHistoryRepository extends JpaRepository<ProductHistory, Long> {
    @Query("""
        select new com.example.duantotnghiep.dto.response.ProductHistoryResponse(
                ph.id,ph.productId,ph.actionType,ph.fieldName,ph.oldValue,ph.newValue,ph.employeeId,e.employeeName,ph.createdDate,ph.note
                ) from ProductHistory ph
                        left join Employee e on e.id = ph.employeeId and e.status = 1
                        where ph.productId = :productId
        """)
    List<ProductHistoryResponse> findAllByProductId(@Param("productId") Long  productId);

}
