// response/StatisticsDashboardResponse.java
package com.example.duantotnghiep.dto.response;

import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter @Setter
public class StatisticsDashboardResponse {
    private BigDecimal todayRevenue;             // today
    private CurrentPeriods currentPeriods;       // tuần/tháng hiện tại & tuần/tháng trước

    private List<AggregationRow> chartAgg;      // theo groupBy
    private List<MonthlyRow>     monthly;       // 12 tháng (nếu yêu cầu)
    private List<YearlyRow>      yearly;        // theo năm (nếu yêu cầu)

    private List<StatusStat>     invoiceStatusStats; // donut
    private List<TopProductRow>  topProducts;        // top N

    private Date periodStart; // khoảng thời gian thực tế dùng để query
    private Date periodEnd;
}
