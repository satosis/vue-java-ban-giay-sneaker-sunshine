package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyRevenueResponse {
    private Integer month;
    private Integer year;
    private Long totalRevenue;
    private Long totalQuantity; // <-- THÊM
}
