package com.example.duantotnghiep.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticDashboardResponse {

    private List<MonthlyRevenueResponse> monthly;
    private List<YearlyRevenueResponse> yearly;
    private List<OrderTypeRevenueResponse> revenueByOrderType;
    private List<TopProductResponse> topProducts;
    private List<InvoiceStatusStatisticResponse> invoiceStatusStats;

    private Long todayRevenue;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    private CurrentPeriodRevenueResponse currentPeriods;

    // === Thêm trường này để FE vẽ biểu đồ (Ngày/Tháng/Năm) ===
    private List<ChartAggItem> chartAgg;

    // === Khai báo ChartAggItem để hết lỗi Cannot resolve symbol ===
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartAggItem {
        private String label;       // ví dụ: "2025-08-31" | "08/2025" | "2025"
        private Long totalRevenue;  // tổng doanh thu bucket
        private Long totalQuantity; // tổng số lượng bucket
    }
}
