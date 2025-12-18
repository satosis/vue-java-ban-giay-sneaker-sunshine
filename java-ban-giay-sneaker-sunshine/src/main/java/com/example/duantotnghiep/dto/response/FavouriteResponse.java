package com.example.duantotnghiep.dto.response;

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
public class FavouriteResponse {
    private Long invoiceId;
    private String invoiceCode;
    private Date deliveredAt;
    private BigDecimal finalAmount;
    private String customerName;
    private List<RatingResponse> invoiceDetail;

}
