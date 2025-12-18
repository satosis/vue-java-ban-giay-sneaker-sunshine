package com.example.duantotnghiep.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CurrentPeriodRevenueResponse {
    private Long weekRevenue;
    private Long monthRevenue;
    private Long quarterRevenue;
    private Long yearRevenue;

    private LocalDateTime weekStart, weekEndExclusive;
    private LocalDateTime monthStart, monthEndExclusive;
    private LocalDateTime quarterStart, quarterEndExclusive;
    private LocalDateTime yearStart, yearEndExclusive;
}

