package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.dto.response.InvoiceTransactionResponse;
import com.example.duantotnghiep.model.InvoiceTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceTransactionRepository extends JpaRepository<InvoiceTransaction,Long> {

    @Query("""
    select new com.example.duantotnghiep.dto.response.InvoiceTransactionResponse(
    it.id,it.invoice.id,it.transactionCode,it.transactionType,it.paymentMethod,it.paymentTime,it.note
    ) from InvoiceTransaction it 
    where it.invoice.id =:invoiceId
""")
    List<InvoiceTransactionResponse> findInvoiceTransactionByIdInvoice(@Param("invoiceId") Long id);
}
