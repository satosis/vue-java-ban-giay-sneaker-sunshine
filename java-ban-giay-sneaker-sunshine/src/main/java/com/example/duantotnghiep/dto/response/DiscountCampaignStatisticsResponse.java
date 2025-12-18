package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountCampaignStatisticsResponse {
    private Long totalInvoices;              // Số hóa đơn
    private BigDecimal totalBeforeDiscount; // Tổng doanh thu trước giảm
    private BigDecimal totalAfterDiscount;  // Tổng doanh thu sau giảm
    private Long totalProductsSold;         // Tổng số sản phẩm bán được
    private Double averageDiscountRate;     // Tỷ lệ chiết khấu trung bình (%)

}


