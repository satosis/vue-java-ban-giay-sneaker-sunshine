package com.example.duantotnghiep.dto.response;

import com.example.duantotnghiep.state.TrangThaiChiTiet;
import com.example.duantotnghiep.state.TrangThaiTong;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InvoiceOnlineResponse {
    private Long invoiceId;
    private String invoiceCode;
    private Long customerId;
    private String customerName;
    private Long employeeId;
    private String employeeName;
    private String phone;
    private String phoneSender;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String description;
    private TrangThaiTong status;
    private TrangThaiChiTiet statusDetail;
    private Date createdDate;
    private Date updatedDate;
    private String deliveryAddress;
    private BigDecimal shippingFee;
    private Date deliveredAt;
    private Boolean isPaid;
    private List<InvoiceTransactionResponse> invoiceTransactionResponses;
    private List<InvoiceDetailOnline> invoiceDetailResponses;

    public InvoiceOnlineResponse(Long invoiceId, String invoiceCode, Long customerId, String customerName, Long employeeId, String employeeName,String phone,String phoneSender, BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal finalAmount, String description, int status, int statusDetail, Date createdDate, Date updatedDate, String deliveryAddress, BigDecimal shippingfee, Date deliveredAt,Boolean isPaid) {
        this.invoiceId = invoiceId;
        this.invoiceCode = invoiceCode;
        this.customerId = customerId;
        this.customerName = customerName;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.phone = phone;
        this.phoneSender = phoneSender;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.description = description;
        this.status = TrangThaiTong.tuMa(status);
        this.statusDetail = TrangThaiChiTiet.tuMa(statusDetail);
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.deliveryAddress = deliveryAddress;
        this.shippingFee = shippingfee;
        this.deliveredAt = deliveredAt;
        this.isPaid = isPaid;
    }
}
