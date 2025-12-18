package com.example.duantotnghiep.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaymentSummary {
    private BigDecimal finalAmount;
    private BigDecimal amountGiven;
    private BigDecimal change;
}
