package com.example.duantotnghiep.dto.request;

import com.example.duantotnghiep.state.TrangThaiChiTiet;
import com.example.duantotnghiep.state.TrangThaiTong;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class InvoiceRequest {
    private CustomerInfo customerInfo;
    private List<CartItemRequest> items;
    private Long employeeId;
    private BigDecimal discountAmount;
    private String description;
    private Integer orderType;
    private TrangThaiTong status;
    private Long voucherId;
    private BigDecimal shippingFee;
    private Boolean isPaid;
    private TrangThaiChiTiet statusDetail;
    private Date deliveredAt;
    private Long discountCampaignId;
    private String voucherCode;
    private Integer request;

}

