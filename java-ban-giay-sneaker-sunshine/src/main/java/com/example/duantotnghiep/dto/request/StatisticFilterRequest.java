package com.example.duantotnghiep.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatisticFilterRequest {
    // Bật/tắt các khối
    private Boolean includeMonthly = true;
    private Boolean includeYearly = true;
    private Boolean includeOrderType = true;
    private Boolean includeTopProducts = true;
    private Boolean includeStatus = true;
    private Boolean includeTodayRevenue = true;
    private Boolean includeCurrentPeriods = false;

    // Tham số sẵn có
    private Integer year;        // dùng cho monthly
    private Integer limit = 5;

    // Phạm vi thời gian (ưu tiên DateTime, thiếu thì dùng Date)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDateTime;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDateTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    // Period tự tính khoảng (nếu thiếu start/end)
    // "week" | "quarter" | "year" | "month"
    private String period;
    private Integer week;        // 1..53 (ISO) khi period=week
    private Integer quarter;     // 1..4   khi period=quarter
    private Integer periodYear;  // năm tham chiếu cho week/quarter/year/month

    // >>> Thêm hai trường này để BE trả chartAgg đúng theo FE <<<
    private String groupBy = "day";      // "day" | "month" | "year"
    private String metric  = "revenue";  // "revenue" | "quantity"
}
