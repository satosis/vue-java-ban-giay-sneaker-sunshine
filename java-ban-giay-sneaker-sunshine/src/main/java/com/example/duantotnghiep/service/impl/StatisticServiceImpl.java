package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.dto.request.EmployeeReportRequest;
import com.example.duantotnghiep.dto.request.StatisticsDashboardRequest;
import com.example.duantotnghiep.dto.response.*;
import com.example.duantotnghiep.model.Invoice;
import com.example.duantotnghiep.repository.InvoiceDetailRepository;
import com.example.duantotnghiep.repository.InvoiceRepository;
import com.example.duantotnghiep.service.StatisticService;
import com.example.duantotnghiep.state.TrangThaiChiTiet;
import com.example.duantotnghiep.state.TrangThaiTong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {

    private final InvoiceRepository invoiceRepo;
    private final InvoiceDetailRepository detailRepo;

    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");
    // Các trạng thái được tính doanh thu
    private static final Set<TrangThaiTong> SUCCESS_SET =
            Set.of(TrangThaiTong.DANG_XU_LY, TrangThaiTong.THANH_CONG);

    // ===== Helpers =====
    private static LocalDate parseYMD(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private static Date startOfDay(LocalDate d) {
        return Date.from(d.atStartOfDay(ZONE_VN).toInstant());
    }

    private static Date endExclusive(LocalDate d) {
        return Date.from(d.plusDays(1).atStartOfDay(ZONE_VN).toInstant());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    @Override
    public StatisticsDashboardResponse getDashboard(StatisticsDashboardRequest req) {
        // ===== 1) Chuẩn hoá input =====
        final int limit = (req.getLimit() != null && req.getLimit() > 0) ? req.getLimit() : 5;
        final String groupBy = (req.getGroupBy() == null) ? "day" : req.getGroupBy();

        LocalDate startD = parseYMD(req.getStartDate());
        LocalDate endD = parseYMD(req.getEndDate());
        LocalDate today = LocalDate.now(ZONE_VN);

        if (startD == null && endD == null) {
            endD = today;
            startD = endD.minusDays(29); // 30 ngày
        } else if (startD != null && endD == null) {
            endD = startD;
        } else if (startD == null) {
            startD = endD;
        }

        Date start = startOfDay(startD);
        Date end = endExclusive(endD);

        StatisticsDashboardResponse res = new StatisticsDashboardResponse();
        res.setPeriodStart(start);
        res.setPeriodEnd(end);

        // ===== 2) Today revenue =====
        if (Boolean.TRUE.equals(req.getIncludeTodayRevenue())) {
            BigDecimal rev = invoiceRepo.sumRevenueBetweenSuccessSet(
                    startOfDay(today), endExclusive(today), SUCCESS_SET);
            res.setTodayRevenue(nz(rev));
        }

        // ===== 3) Current periods: tuần/tháng hiện tại + tuần/tháng trước =====
        if (Boolean.TRUE.equals(req.getIncludeCurrentPeriods())) {
            // Tuần hiện tại: Mon..Sun
            LocalDate weekStart = today.with(DayOfWeek.MONDAY);
            LocalDate weekEndEx = weekStart.plusWeeks(1);

            BigDecimal weekRev = invoiceRepo.sumRevenueBetweenSuccessSet(
                    startOfDay(weekStart), endExclusive(weekEndEx.minusDays(1)), SUCCESS_SET);

            LocalDate prevWeekStart = weekStart.minusWeeks(1);
            LocalDate prevWeekEndEx = weekStart;

            BigDecimal prevWeekRev = invoiceRepo.sumRevenueBetweenSuccessSet(
                    startOfDay(prevWeekStart), endExclusive(prevWeekEndEx.minusDays(1)), SUCCESS_SET);

            // Tháng hiện tại
            LocalDate mStart = today.withDayOfMonth(1);
            LocalDate mEndEx = mStart.plusMonths(1);

            BigDecimal monthRev = invoiceRepo.sumRevenueBetweenSuccessSet(
                    startOfDay(mStart), endExclusive(mEndEx.minusDays(1)), SUCCESS_SET);

            // Tháng trước
            LocalDate pmStart = mStart.minusMonths(1);
            LocalDate pmEndEx = mStart;

            BigDecimal prevMonthRev = invoiceRepo.sumRevenueBetweenSuccessSet(
                    startOfDay(pmStart), endExclusive(pmEndEx.minusDays(1)), SUCCESS_SET);

            res.setCurrentPeriods(new CurrentPeriods(
                    nz(weekRev), nz(prevWeekRev), nz(monthRev), nz(prevMonthRev)
            ));
        }

        // ===== 4) chartAgg theo groupBy =====
        List<AggregationRow> chartAgg;
        switch (groupBy) {
            case "month" -> chartAgg = buildMonthlyAggregation(start, end);
            case "year" -> chartAgg = buildYearlyAggregation(start, end);
            default -> chartAgg = buildDailyAggregation(startD, endD);
        }
        res.setChartAgg(chartAgg);

        // ===== 5) Monthly tổng hợp (nếu yêu cầu) =====
        if (Boolean.TRUE.equals(req.getIncludeMonthly())) {
            List<MonthlyRow> monthly = invoiceRepo.aggregateMonthlySet(start, end, SUCCESS_SET)
                    .stream()
                    .map(a -> new MonthlyRow(
                            ((Number) a[0]).intValue(),       // year
                            ((Number) a[1]).intValue(),       // month
                            (BigDecimal) a[2],                // revenue
                            ((Number) a[3]).longValue()       // quantity = count invoice
                    )).toList();
            res.setMonthly(monthly);
        }

        // ===== 6) Yearly tổng hợp (nếu yêu cầu) =====
        if (Boolean.TRUE.equals(req.getIncludeYearly())) {
            List<YearlyRow> yearly = invoiceRepo.aggregateYearlySet(start, end, SUCCESS_SET)
                    .stream()
                    .map(a -> new YearlyRow(
                            ((Number) a[0]).intValue(),
                            (BigDecimal) a[1],
                            ((Number) a[2]).longValue()
                    )).toList();
            res.setYearly(yearly);
        }

        // ===== 7) Donut theo TrangThaiTong (tất cả trạng thái) =====
        if (Boolean.TRUE.equals(req.getIncludeStatus())) {
            List<StatusStat> donut = invoiceRepo.countByStatus(start, end).stream()
                    .map(a -> {
                        TrangThaiTong st = (TrangThaiTong) a[0];
                        long cnt = ((Number) a[1]).longValue();
                        Integer code = (st == null) ? null : st.getMa();
                        String name = (st == null) ? "Khác" : st.getMoTa();
                        return new StatusStat(code, name, cnt);
                    }).toList();
            res.setInvoiceStatusStats(donut);
        }

        // ===== 8) Top sản phẩm bán chạy =====
        if (Boolean.TRUE.equals(req.getIncludeTopProducts())) {
            List<TopProductRow> tops = detailRepo.topProducts(start, end, SUCCESS_SET)
                    .stream()
                    .limit(limit)
                    .map(a -> new TopProductRow(
                            a[0] == null ? null : ((Number) a[0]).longValue(),
                            (String) a[1],
                            a[2] == null ? 0L : ((Number) a[2]).longValue()
                    )).toList();
            res.setTopProducts(tops);
        }

        return res;
    }

    // ===== Aggregations =====

    // Day-level: group tại Java (portable)
    private List<AggregationRow> buildDailyAggregation(LocalDate startD, LocalDate endD) {
        Date s = startOfDay(startD);
        Date e = endExclusive(endD);

        // Tập ngày (inclusive FE)
        List<LocalDate> days = startD.datesUntil(endD.plusDays(1)).toList();

        List<Invoice> invoices = invoiceRepo.findSuccessBetweenSet(s, e, SUCCESS_SET);

        Map<LocalDate, List<Invoice>> byDay = invoices.stream().collect(Collectors.groupingBy(
                inv -> inv.getCreatedDate().toInstant().atZone(ZONE_VN).toLocalDate()
        ));

        return days.stream().map(d -> {
            List<Invoice> lst = byDay.getOrDefault(d, Collections.emptyList());
            BigDecimal rev = lst.stream()
                    .map(Invoice::getFinalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long qty = lst.size(); // nếu muốn "tổng SP bán": đổi sang SUM(InvoiceDetail.quantity)
            return new AggregationRow(d.toString(), nz(rev), qty);
        }).toList();
    }

    private List<AggregationRow> buildMonthlyAggregation(Date start, Date end) {
        return invoiceRepo.aggregateMonthlySet(start, end, SUCCESS_SET).stream()
                .map(a -> {
                    int y = ((Number) a[0]).intValue();
                    int m = ((Number) a[1]).intValue();
                    String label = String.format("%02d/%d", m, y);
                    BigDecimal rev = (BigDecimal) a[2];
                    long qty = ((Number) a[3]).longValue();
                    return new AggregationRow(label, nz(rev), qty);
                }).toList();
    }

    private List<AggregationRow> buildYearlyAggregation(Date start, Date end) {
        return invoiceRepo.aggregateYearlySet(start, end, SUCCESS_SET).stream()
                .map(a -> {
                    int y = ((Number) a[0]).intValue();
                    BigDecimal rev = (BigDecimal) a[1];
                    long qty = ((Number) a[2]).longValue();
                    return new AggregationRow(String.valueOf(y), nz(rev), qty);
                }).toList();
    }

    @Override
    public List<EmployeeReportDto> getEmployeeSalesReport(EmployeeReportRequest request) {
        List<Object[]> rawResults = invoiceRepo.getEmployeeSalesReportNative(request.getEmployeeId(), request.getStartDate(), request.getEndDate());

        return rawResults.stream().map(result -> {
            int i = 0;
            return new EmployeeReportDto(
                    ((Long) result[i++]),
                    (String) result[i++],                    // employeeName
                    ((Number) result[i++]).intValue(),       // totalInvoices
                    ((Number) result[i++]).intValue(),       // totalProducts
                    (BigDecimal) result[i++],                // totalRevenue
                    ((Number) result[i++]).intValue(),       // successInvoices
                    ((Number) result[i++]).intValue(),       // successProducts
                    (BigDecimal) result[i++],                // successRevenue
                    ((Number) result[i++]).intValue(),       // cancelledInvoices
                    ((Number) result[i++]).intValue(),       // cancelledProducts
                    (BigDecimal) result[i++]                 // cancelledRevenue
            );
        }).collect(Collectors.toList());
    }


}

