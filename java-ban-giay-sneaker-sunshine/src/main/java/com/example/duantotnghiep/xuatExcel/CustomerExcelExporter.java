package com.example.duantotnghiep.xuatExcel;

import com.example.duantotnghiep.dto.response.CustomerResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CustomerExcelExporter {

    private static final SimpleDateFormat DOB_FMT = new SimpleDateFormat("dd/MM/yyyy");
    private static final DateTimeFormatter LDT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static ByteArrayInputStream exportCustomerToExcel(List<CustomerResponse> data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Customers");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle   = createDataStyle(workbook);
            CellStyle dateStyle   = createDateStyle(workbook);

            String[] headers = {
                    "ID", "Mã KH", "Tên KH", "Email", "SĐT",
                    "Giới tính", "Ngày sinh",
                    "Quốc gia", "Tỉnh/TP", "Quận/Huyện", "Phường/Xã", "Địa chỉ",
                    "Trạng thái", "Điểm uy tín", "Bị cấm?", "Lý do cấm",
                    "Ngày tạo"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (CustomerResponse c : data) {
                Row row = sheet.createRow(rowIdx++);

                createCell(row, 0,  c.getId(), dataStyle);
                createCell(row, 1,  c.getCustomerCode(), dataStyle);
                createCell(row, 2,  c.getCustomerName(), dataStyle);
                createCell(row, 3,  c.getEmail(), dataStyle);
                createCell(row, 4,  c.getPhone(), dataStyle);

                createCell(row, 5,  genderText(c.getGender()), dataStyle);
                createCell(row, 6,  c.getDateOfBirth() != null ? DOB_FMT.format(c.getDateOfBirth()) : "", dateStyle);

                createCell(row, 7,  c.getCountry(), dataStyle);
                createCell(row, 8,  c.getProvinceName(), dataStyle);
                createCell(row, 9,  c.getDistrictName(), dataStyle);
                createCell(row, 10, c.getWardName(), dataStyle);
                createCell(row, 11, c.getHouseName(), dataStyle);

                createCell(row, 12, statusText(c.getStatus()), dataStyle);
                createCell(row, 13, c.getTrustScore(), dataStyle);
                createCell(row, 14, boolText(c.getIsBlacklisted()), dataStyle);
                createCell(row, 15, c.getBlacklistReason(), dataStyle);

                createCell(row, 16, c.getCreatedDate() != null ? c.getCreatedDate().format(LDT_FMT) : "", dateStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3000) sheet.setColumnWidth(i, 3000);
                if (sheet.getColumnWidth(i) > 15000) sheet.setColumnWidth(i, 15000);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // ===== Helpers =====
    private static String genderText(Integer g) {
        if (g == null) return "Không rõ";
        return switch (g) { case 1 -> "Nam"; case 2 -> "Nữ"; default -> "Khác"; };
    }
    private static String statusText(Integer s) {
        if (s == null) return "Không rõ";
        return s == 1 ? "Hoạt động" : "Ngừng";
    }
    private static String boolText(Boolean b) {
        if (b == null) return "Không";
        return b ? "Có" : "Không";
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        XSSFCellStyle st = (XSSFCellStyle) wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        st.setFont(font);
        st.setFillForegroundColor(new XSSFColor(new java.awt.Color(135,206,235), null));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setBorderTop(BorderStyle.THIN);
        st.setBorderBottom(BorderStyle.THIN);
        st.setBorderLeft(BorderStyle.THIN);
        st.setBorderRight(BorderStyle.THIN);
        st.setAlignment(HorizontalAlignment.CENTER);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        return st;
    }
    private static CellStyle createDataStyle(Workbook wb) {
        CellStyle st = wb.createCellStyle();
        st.setBorderTop(BorderStyle.THIN);
        st.setBorderBottom(BorderStyle.THIN);
        st.setBorderLeft(BorderStyle.THIN);
        st.setBorderRight(BorderStyle.THIN);
        st.setAlignment(HorizontalAlignment.CENTER);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        return st;
    }
    private static CellStyle createDateStyle(Workbook wb) {
        CellStyle st = wb.createCellStyle();
        CreationHelper ch = wb.getCreationHelper();
        st.setDataFormat(ch.createDataFormat().getFormat("dd/MM/yyyy HH:mm:ss"));
        st.setBorderTop(BorderStyle.THIN);
        st.setBorderBottom(BorderStyle.THIN);
        st.setBorderLeft(BorderStyle.THIN);
        st.setBorderRight(BorderStyle.THIN);
        st.setAlignment(HorizontalAlignment.CENTER);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        return st;
    }
    private static void createCell(Row row, int col, Object val, CellStyle st) {
        Cell cell = row.createCell(col);
        if (val instanceof Number) cell.setCellValue(((Number) val).doubleValue());
        else cell.setCellValue(val != null ? val.toString() : "");
        cell.setCellStyle(st);
    }
}
