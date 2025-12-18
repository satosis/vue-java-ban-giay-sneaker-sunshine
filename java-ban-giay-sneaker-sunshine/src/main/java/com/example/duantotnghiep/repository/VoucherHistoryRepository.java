package com.example.duantotnghiep.repository;

import com.example.duantotnghiep.model.Customer;
import com.example.duantotnghiep.model.Invoice;
import com.example.duantotnghiep.model.Voucher;
import com.example.duantotnghiep.model.VoucherHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface VoucherHistoryRepository extends JpaRepository<VoucherHistory, Long> {
    @Query("SELECT vh FROM VoucherHistory vh WHERE vh.customer.id = :customerId")
    List<VoucherHistory> findByCustomerId(@Param("customerId") Long customerId);

    @Query("select vh from VoucherHistory vh where vh.voucher.voucherCode = :code and vh.customer.id = :customerId")
    Optional<VoucherHistory> findByKhachHang(@Param("code") String code, @Param("customerId") Long customerId);

    List<VoucherHistory> findByCustomerIdAndStatus(Long customerId, Integer status);

    // Trả về nhiều lịch sử voucher nếu có
    List<VoucherHistory> findByInvoiceAndVoucherAndStatus(Invoice invoice, Voucher voucher, int status);

    boolean existsByVoucherAndCustomerAndInvoiceNot(Voucher voucher, Customer customer, Invoice invoice);

    boolean existsByVoucherAndCustomer(Voucher voucher, Customer customer);

//    @Query("""
//      select distinct h.voucher.id
//      from VoucherHistory h
//      where h.customer.id = :customerId
//        and h.status in :statuses
//        and (h.invoice.id <> :invoiceId)
//    """)
//    Set<Long> findVoucherIdsUsedByCustomerExcludingInvoice(
//            @Param("customerId") Long customerId,
//            @Param("invoiceId") Long invoiceId,
//            @Param("statuses") Collection<Integer> statuses
//    );
//
//    List<VoucherHistory> findByInvoice(Invoice invoice);
//
//    Optional<VoucherHistory> findTopByInvoiceAndVoucherAndStatus(Invoice invoice, Voucher voucher, Integer status);

    boolean existsByVoucherAndCustomerAndStatusAndInvoiceNot(
            Voucher voucher, Customer customer, Integer status, Invoice invoice);

    // Lấy danh sách voucher_id KH đã dùng (status=1) bên ngoài invoice hiện tại
    @Query("""
      select distinct h.voucher.id
      from VoucherHistory h
      where h.customer.id = :customerId
        and h.status = 1
        and h.invoice.id <> :invoiceId
    """)
    Set<Long> findVoucherIdsUsedByCustomerExcludingInvoice(
            @Param("customerId") Long customerId,
            @Param("invoiceId") Long invoiceId
    );

    Optional<VoucherHistory> findTopByInvoiceAndVoucherAndStatus(Invoice invoice, Voucher voucher, Integer status);

    List<VoucherHistory> findByInvoice(Invoice invoice);

}