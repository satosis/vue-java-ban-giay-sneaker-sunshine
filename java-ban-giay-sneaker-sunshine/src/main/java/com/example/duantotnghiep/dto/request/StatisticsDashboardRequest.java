package com.example.duantotnghiep.dto.request;


import lombok.Getter; import lombok.Setter;

@Getter @Setter
public class StatisticsDashboardRequest {
    private Boolean includeMonthly;
    private Boolean includeYearly;
    private Boolean includeTodayRevenue;
    private Boolean includeTopProducts;
    private Boolean includeStatus;
    private Boolean includeCurrentPeriods;

    private Integer limit;           // default 5
    private String  groupBy;         // "day" | "month" | "year"
    private String  metric;          // "revenue" | "quantity"
    private String  startDate;       // "YYYY-MM-DD"
    private String  endDate;         // "YYYY-MM-DD"
}
