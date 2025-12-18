package com.example.duantotnghiep.service;

import com.example.duantotnghiep.dto.PaymentSummary;
import com.example.duantotnghiep.dto.request.InvoiceRequest;
import com.example.duantotnghiep.dto.request.InvoiceSearchRequest;
import com.example.duantotnghiep.dto.response.*;
import com.example.duantotnghiep.model.Customer;
import com.example.duantotnghiep.model.Invoice;
import com.example.duantotnghiep.model.PromotionSuggestion;
import com.example.duantotnghiep.state.TrangThaiChiTiet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface InvoiceService {

    @Transactional
    InvoiceResponse createEmptyInvoice();

    List<CustomerResponse> findCustomersByPhonePrefix(String phonePrefix);

    @Transactional
    CustomerResponse createQuickCustomer(String phone, String name, String email);

    PaymentSummary calculatePayment(Long invoiceId, BigDecimal amountGiven);

    @Transactional
    void checkout(Long invoiceId);

    @Transactional
    void clearCart(Long invoiceId);

    InvoiceDisplayResponse getInvoiceWithDetails(Long invoiceId);

    Page<InvoiceDisplayResponse> getInvoiceDisplays(Pageable pageable);


    Page<InvoiceResponse> searchSeparatedStatus(String keyword,
                                                String counterStatusKey,
                                                String onlineStatusKey,
                                                LocalDate createdDate,
                                                Pageable pageable);

    Invoice findByInvoiceCode(String code);

    List<InvoiceDisplayResponse> getAllInvoicesWithDetails();

    Invoice findById(Long id);

    List<InvoiceDisplayResponse> getInvoicesWithDetailsByIds(List<Long> ids);

    @Transactional
    InvoiceDisplayResponse createInvoice(InvoiceRequest request);


    void autoBlacklistIfTooManyCancellations(Customer customer);
    void resetBlacklistState(Customer customer, LocalDateTime now);
    @Transactional
    InvoiceWithZaloPayResponse createInvoiceAndZaloPay(InvoiceRequest request) throws Exception;

    Invoice getInvoice(String appTransId);

    InvoiceDisplayResponse createInvoiceShipCode(InvoiceRequest request);

    void updateInvoiceStatusByAppTransId(String appTransId, Integer status, Integer statusDetail,Boolean isPaid);

    @Transactional
    InvoiceWithVnpayResponse createInvoiceAndVnpay(InvoiceRequest request) throws Exception;
    void updateStatusIfPaid(String appTransId) throws Exception;

    List<InvoiceResponse> searchInvoices(InvoiceSearchRequest request);

    PromotionSuggestion getSuggestedPromotion(Long customerId);

    @Transactional
    void processInvoicePayment(Long invoiceId);

    void checkAndGiftVoucher(Long customerId);

    TrangThaiChiTiet findStatusByCode(String code);
    List<Integer> findDiscountCampianByCode(String code);

}
