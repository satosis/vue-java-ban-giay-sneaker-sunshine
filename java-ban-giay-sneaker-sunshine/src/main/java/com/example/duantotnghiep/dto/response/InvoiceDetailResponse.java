package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InvoiceDetailResponse {
    private Long id;
    private String productName;
    private String productCode;
    private String categoryName;
    private SizeResponse size;
    private ColorResponse color;
    private BigDecimal sellPrice;
    private Integer discountPercentage;     // % giảm
    private BigDecimal discountAmount;      // Số tiền giảm
    private BigDecimal discountedPrice;     // Giá sau giảm
    private Integer quantity;
    private String invoiceCodeDetail;
    private BigDecimal totalPrice;          // Tổng giá gốc = sellPrice * quantity
    private BigDecimal finalTotalPrice;     // Tổng giá sau giảm = discountedPrice * quantity
    private String customerName;
    private String deliveryAddress;

}

