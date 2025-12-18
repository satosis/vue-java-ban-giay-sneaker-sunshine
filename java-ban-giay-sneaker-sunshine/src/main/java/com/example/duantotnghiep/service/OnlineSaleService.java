package com.example.duantotnghiep.service;

import com.example.duantotnghiep.dto.request.UpdateAddress;
import com.example.duantotnghiep.dto.response.InvoiceOnlineResponse;
import com.example.duantotnghiep.dto.response.OrderStatusHistoryResponse;
import com.example.duantotnghiep.dto.response.StatusCountDTO;
import com.example.duantotnghiep.dto.response.StatusCountResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface OnlineSaleService {

    void chuyenTrangThai(Long invoiceId, String nextKey);
    void huyDonEmployee(Long invoiceId, String nextKey);
    void huyDonClient(Long invoiceId, String nextKey);
    InvoiceOnlineResponse getOrder(Long invoiceId);

    void huyDonVaHoanTienClient(Long invoiceId,String nextKey,String note,Integer request,Boolean isPaid);
    void huyDonVaHoanTienEmployee(Long invoiceId,String nextKey,String note,String paymentMenthod,Boolean isPaid,String tradeCode,String bankName);
    void giaoHangThatBaiVaHoanTien(Long invoiceId, String nextKey, String note, String paymentMenthod, Boolean isPaid, String tradeCode,String bankName);
    List<StatusCountResponse> getCountByStatusDetail();

    InvoiceOnlineResponse getOrderByCustomer(Long invoiceId);

    List<InvoiceOnlineResponse> getOrderByCustomer2(Integer status);
    List<InvoiceOnlineResponse> getOrderByCustomer3(Integer status);

    List<OrderStatusHistoryResponse> getOrderStatusHistory(Long invoiceId);

    void updateAddressShipping(UpdateAddress address);
    void updateSDT(Long invoiceId,String phone);

    List<StatusCountDTO> getCountByStatus();
    List<StatusCountDTO> getCountByStatusV2();

    BigDecimal getRevenue(String type);


}