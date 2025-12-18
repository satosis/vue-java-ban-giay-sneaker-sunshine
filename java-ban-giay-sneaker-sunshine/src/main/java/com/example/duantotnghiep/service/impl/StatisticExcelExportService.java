package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.dto.request.EmployeeReportRequest;
import com.example.duantotnghiep.dto.request.StatisticsDashboardRequest;
import com.example.duantotnghiep.dto.response.*;
import com.example.duantotnghiep.service.StatisticService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StatisticExcelExportService {

    private final StatisticService statisticService;

    private static final Locale VI = new Locale("vi", "VN");
    private static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");

    // =================== PUBLIC APIs ===================

    /** Xuất Excel tổng quan dashboard dựa trên dữ liệu trả về từ StatisticServiceImpl (đúng theo code bạn gửi). */
    public void exportDashboardExcel(HttpServletResponse response, StatisticsDashboardRequest req) throws IOException {
        Objects.requireNonNull(response, "response is required");
        StatisticsDashboardResponse data = statisticService.getDashboard(req);

        try (Workbook wb = new XSSFWorkbook()) {
            Styles st = createStyles(wb);

            // Sheet 1: Tổng quan + KPI
            Sheet s1 = wb.createSheet("Tong quan");
            int row = 0;
            row = titleBlock(s1, row, st, "THỐNG KÊ TỔNG QUAN");

            String fromStr = formatDate(data.getPeriodStart());
            String toStr = formatDateExclusive(data.getPeriodEnd()); // endExclusive -> hiển thị lùi 1 ngày
            row = filterBlock(s1, row, st,
                    "Từ ngày", fromStr,
                    "Đến ngày", toStr,
                    "Nhóm theo", req.getGroupBy() == null ? "day" : req.getGroupBy());
            row++;

            Row h = s1.createRow(row++);
            setHeader(h, st.header, "Chỉ số", "Giá trị");

            if (data.getTodayRevenue() != null) {
                Row r = s1.createRow(row++);
                setCell(r, 0, "Doanh thu hôm nay", st.bold);
                setCell(r, 1, nz(data.getTodayRevenue()), st.currency);
            }

            if (data.getCurrentPeriods() != null) {
                CurrentPeriods cp = data.getCurrentPeriods();
                row = kpiRow(s1, row, st, "Tuần này", cp.getWeekRevenue());
                row = kpiRow(s1, row, st, "Tuần trước", cp.getPrevWeekRevenue());
                row = kpiRow(s1, row, st, "Tháng này", cp.getMonthRevenue());
                row = kpiRow(s1, row, st, "Tháng trước", cp.getPrevMonthRevenue());
            }

            autosizeAll(s1, 2);

            // Sheet 2: Biểu đồ (chartAgg)
            Sheet s2 = wb.createSheet("Bieu do");
            writeChartAgg(s2, st, data.getChartAgg());
            autosizeAll(s2, 3);

            // Sheet 3: Theo tháng (monthly)
            Sheet s3 = wb.createSheet("Theo thang");
            writeMonthly(s3, st, data.getMonthly());
            autosizeAll(s3, 4);

            // Sheet 4: Theo năm (yearly)
            Sheet s4 = wb.createSheet("Theo nam");
            writeYearly(s4, st, data.getYearly());
            autosizeAll(s4, 3);

            // Sheet 5: Trạng thái đơn
            Sheet s5 = wb.createSheet("Trang thai");
            writeStatuses(s5, st, data.getInvoiceStatusStats());
            autosizeAll(s5, 3);

            // Sheet 6: Top sản phẩm
            Sheet s6 = wb.createSheet("Top san pham");
            writeTopProducts(s6, st, data.getTopProducts());
            autosizeAll(s6, 3);

            // Ghi response
            writeToResponse(response, wb, buildFileName("dashboard"));
        }
    }

    /** Xuất Excel báo cáo theo nhân viên dựa trên getEmployeeSalesReport(EmployeeReportRequest). */
    public void exportEmployeeReportExcel(HttpServletResponse response, EmployeeReportRequest req) throws IOException {
        Objects.requireNonNull(response, "response is required");
        List<EmployeeReportDto> rows = statisticService.getEmployeeSalesReport(req);

        try (Workbook wb = new XSSFWorkbook()) {
            Styles st = createStyles(wb);

            Sheet sh = wb.createSheet("Nhan vien");
            int row = 0;

            row = titleBlock(sh, row, st, "BÁO CÁO DOANH SỐ THEO NHÂN VIÊN");

            Row h = sh.createRow(row++);
            setHeader(h, st.header,
                    "Employee ID", "Tên nhân viên",
                    "Tổng HĐ", "Tổng SP", "Tổng doanh thu",
                    "HĐ thành công", "SP thành công", "Doanh thu thành công",
                    "HĐ hủy", "SP hủy", "Doanh thu hủy");

            long sumInv = 0, sumProd = 0, sumSuccInv = 0, sumSuccProd = 0, sumCancelInv = 0, sumCancelProd = 0;
            BigDecimal sumRev = BigDecimal.ZERO, sumSuccRev = BigDecimal.ZERO, sumCancelRev = BigDecimal.ZERO;

            for (EmployeeReportDto d : rows) {
                Row r = sh.createRow(row++);
                int c = 0;
                setCell(r, c++, d.getEmployeeId(), st.number);
                setCell(r, c++, d.getEmployeeName(), st.text);
                setCell(r, c++, d.getTotalInvoices(), st.number);
                setCell(r, c++, d.getTotalProducts(), st.number);
                setCell(r, c++, nz(d.getTotalRevenue()), st.currency);
                setCell(r, c++, d.getSuccessInvoices(), st.number);
                setCell(r, c++, d.getSuccessProducts(), st.number);
                setCell(r, c++, nz(d.getSuccessRevenue()), st.currency);
                setCell(r, c++, d.getCancelledInvoices(), st.number);
                setCell(r, c++, d.getCancelledProducts(), st.number);
                setCell(r, c++, nz(d.getCancelledRevenue()), st.currency);

                sumInv += nvl(d.getTotalInvoices());
                sumProd += nvl(d.getTotalProducts());
                sumSuccInv += nvl(d.getSuccessInvoices());
                sumSuccProd += nvl(d.getSuccessProducts());
                sumCancelInv += nvl(d.getCancelledInvoices());
                sumCancelProd += nvl(d.getCancelledProducts());

                sumRev = sumRev.add(nz(d.getTotalRevenue()));
                sumSuccRev = sumSuccRev.add(nz(d.getSuccessRevenue()));
                sumCancelRev = sumCancelRev.add(nz(d.getCancelledRevenue()));
            }

            Row total = sh.createRow(row++);
            int c = 0;
            setCell(total, c++, "", st.totalLabel);
            setCell(total, c++, "TỔNG CỘNG", st.totalLabel);
            setCell(total, c++, sumInv, st.totalNumber);
            setCell(total, c++, sumProd, st.totalNumber);
            setCell(total, c++, sumRev, st.totalCurrency);
            setCell(total, c++, sumSuccInv, st.totalNumber);
            setCell(total, c++, sumSuccProd, st.totalNumber);
            setCell(total, c++, sumSuccRev, st.totalCurrency);
            setCell(total, c++, sumCancelInv, st.totalNumber);
            setCell(total, c++, sumCancelProd, st.totalNumber);
            setCell(total, c++, sumCancelRev, st.totalCurrency);

            autosizeAll(sh, 11);
            writeToResponse(response, wb, buildFileName("employee-report"));
        }
    }

    // =================== WRITERS (Dashboard) ===================

    private void writeChartAgg(Sheet s, Styles st, List<AggregationRow> list) {
        int row = 0;
        row = titleBlock(s, row, st, "CHI TIẾT THEO NHÃN (chartAgg)");
        Row h = s.createRow(row++);
        setHeader(h, st.header, "Nhãn", "Doanh thu", "Số lượng");
        if (list != null) {
            for (AggregationRow a : list) {
                Row r = s.createRow(row++);
                setCell(r, 0, a.getLabel(), st.text);

            }
        }
    }

    private void writeMonthly(Sheet s, Styles st, List<MonthlyRow> list) {
        int row = 0;
        row = titleBlock(s, row, st, "TỔNG HỢP THEO THÁNG");
        Row h = s.createRow(row++);
        setHeader(h, st.header, "Năm", "Tháng", "Doanh thu", "Số đơn");
        if (list != null) {
            for (MonthlyRow a : list) {
                Row r = s.createRow(row++);
                setCell(r, 0, a.getYear(), st.number);
                setCell(r, 1, a.getMonth(), st.number);
            }
        }
    }

    private void writeYearly(Sheet s, Styles st, List<YearlyRow> list) {
        int row = 0;
        row = titleBlock(s, row, st, "TỔNG HỢP THEO NĂM");
        Row h = s.createRow(row++);
        setHeader(h, st.header, "Năm", "Doanh thu", "Số đơn");
        if (list != null) {
            for (YearlyRow a : list) {
                Row r = s.createRow(row++);
                setCell(r, 0, a.getYear(), st.number);
            }
        }
    }

    private void writeStatuses(Sheet s, Styles st, List<StatusStat> list) {
        int row = 0;
        row = titleBlock(s, row, st, "THỐNG KÊ THEO TRẠNG THÁI");
        Row h = s.createRow(row++);
        setHeader(h, st.header, "Mã", "Trạng thái", "Số lượng");
        if (list != null) {
            for (StatusStat a : list) {
                Row r = s.createRow(row++);
            }
        }
    }

    private void writeTopProducts(Sheet s, Styles st, List<TopProductRow> list) {
        int row = 0;
        row = titleBlock(s, row, st, "TOP SẢN PHẨM BÁN CHẠY");
        Row h = s.createRow(row++);
        setHeader(h, st.header, "ProductDetail ID", "Tên sản phẩm", "Số lượng bán");
        if (list != null) {
            for (TopProductRow a : list) {
                Row r = s.createRow(row++);
                setCell(r, 1, a.getProductName(), st.text);
            }
        }
    }

    // =================== STYLES & UTIL ===================

    private record Styles(
            CellStyle header, CellStyle title,
            CellStyle text, CellStyle bold,
            CellStyle number, CellStyle currency,
            CellStyle totalLabel, CellStyle totalNumber, CellStyle totalCurrency
    ) {}

    private Styles createStyles(Workbook wb) {
        DataFormat df = wb.createDataFormat();

        Font fontBold = wb.createFont();
        fontBold.setBold(true);

        Font fontTitle = wb.createFont();
        fontTitle.setBold(true);
        fontTitle.setFontHeightInPoints((short) 14);

        CellStyle header = wb.createCellStyle();
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setBorderTop(BorderStyle.THIN);
        header.setBorderBottom(BorderStyle.THIN);
        header.setBorderLeft(BorderStyle.THIN);
        header.setBorderRight(BorderStyle.THIN);
        header.setFont(fontBold);

        CellStyle title = wb.createCellStyle();
        title.setAlignment(HorizontalAlignment.LEFT);
        title.setVerticalAlignment(VerticalAlignment.CENTER);
        title.setFont(fontTitle);

        CellStyle text = wb.createCellStyle();
        text.setAlignment(HorizontalAlignment.LEFT);
        text.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle bold = wb.createCellStyle();
        bold.cloneStyleFrom(text);
        bold.setFont(fontBold);

        CellStyle number = wb.createCellStyle();
        number.setAlignment(HorizontalAlignment.RIGHT);
        number.setDataFormat(df.getFormat("#,##0"));

        CellStyle currency = wb.createCellStyle();
        currency.setAlignment(HorizontalAlignment.RIGHT);
        currency.setDataFormat(df.getFormat("#,##0"));

        CellStyle totalLabel = wb.createCellStyle();
        totalLabel.cloneStyleFrom(bold);

        CellStyle totalNumber = wb.createCellStyle();
        totalNumber.cloneStyleFrom(number);
        totalNumber.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        totalNumber.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle totalCurrency = wb.createCellStyle();
        totalCurrency.cloneStyleFrom(currency);
        totalCurrency.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        totalCurrency.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return new Styles(header, title, text, bold, number, currency, totalLabel, totalNumber, totalCurrency);
    }

    private int titleBlock(Sheet s, int row, Styles st, String title) {
        Row r = s.createRow(row++);
        setCell(r, 0, title, st.title);
        return row;
    }

    /** Viết 3 cặp label/value thành 2 dòng: L1|V1 | L2|V2 | L3|V3 */
    private int filterBlock(Sheet s, int row, Styles st,
                            String l1, String v1, String l2, String v2, String l3, String v3) {
        Row r1 = s.createRow(row++);
        Row r2 = s.createRow(row++);
        setCell(r1, 0, l1, st.bold); setCell(r1, 1, v1, st.text);
        setCell(r1, 2, l2, st.bold); setCell(r1, 3, v2, st.text);
        setCell(r1, 4, l3, st.bold); setCell(r1, 5, v3, st.text);
        return row;
    }

    private void setHeader(Row row, CellStyle st, String... labels) {
        for (int i = 0; i < labels.length; i++) setCell(row, i, labels[i], st);
    }

    private void setCell(Row row, int col, String val, CellStyle st) {
        Cell c = row.createCell(col, CellType.STRING);
        c.setCellValue(val == null ? "" : val);
        c.setCellStyle(st);
    }

    private void setCell(Row row, int col, Integer val, CellStyle st) {
        if (val == null) { setCell(row, col, "", st); return; }
        Cell c = row.createCell(col, CellType.NUMERIC);
        c.setCellValue(val);
        c.setCellStyle(st);
    }

    private void setCell(Row row, int col, Long val, CellStyle st) {
        if (val == null) { setCell(row, col, "", st); return; }
        Cell c = row.createCell(col, CellType.NUMERIC);
        c.setCellValue(val);
        c.setCellStyle(st);
    }

    private void setCell(Row row, int col, BigDecimal val, CellStyle st) {
        Cell c = row.createCell(col, CellType.NUMERIC);
        c.setCellValue(nz(val).doubleValue());
        c.setCellStyle(st);
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private int nvl(Integer v) { return v == null ? 0 : v; }
    private String nz(String v) { return v == null ? "" : v; }

    private void autosizeAll(Sheet s, int cols) {
        for (int i = 0; i < cols; i++) {
            s.autoSizeColumn(i);
            int width = s.getColumnWidth(i);
            s.setColumnWidth(i, Math.min(width + 512, 255 * 256)); // +2 ký tự
        }
    }

    private String buildFileName(String prefix) {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String base = prefix + "_stat_" + ts + ".xlsx";
        return URLEncoder.encode(base, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private void writeToResponse(HttpServletResponse resp, Workbook wb, String filename) throws IOException {
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
        wb.write(resp.getOutputStream()); // không đóng outputStream của container
    }

    private String formatDate(Date d) {
        if (d == null) return "";
        return new SimpleDateFormat("dd/MM/yyyy").format(d);
    }

    private String formatDateExclusive(Date endExclusive) {
        if (endExclusive == null) return "";
        // endExclusive là startOfDay(nextDay) -> hiển thị (nextDay - 1)
        long millis = endExclusive.toInstant().atZone(ZONE_VN).minusDays(1).toInstant().toEpochMilli();
        return new SimpleDateFormat("dd/MM/yyyy").format(new Date(millis));
    }

    private int kpiRow(Sheet s, int row, Styles st, String label, BigDecimal value) {
        Row r = s.createRow(row++);
        setCell(r, 0, label, st.text);
        setCell(r, 1, nz(value), st.currency);
        return row;
    }
}
