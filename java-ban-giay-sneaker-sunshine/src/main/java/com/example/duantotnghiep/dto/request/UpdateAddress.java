package com.example.duantotnghiep.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdateAddress {
    private Long invoiceId;
    private String newAddress;
    private BigDecimal shippingFee;
    private BigDecimal finalAmount;
}
