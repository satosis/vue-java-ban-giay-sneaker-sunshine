package com.example.duantotnghiep.xuatExcel;

import com.example.duantotnghiep.model.Voucher;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class VoucherExportExcel {

    // Giữ nguyên tên để không phải đổi service
    public static ByteArrayInputStream exportProductToExcel(List<Voucher> data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Vouchers");

            // Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // Header
            String[] headers = {
                    "ID", "Tên khách hàng", "Tên Nhân viên tạo", "Mã voucher", "Tên voucher",
                    "Phần trăm giảm", "Số lượng", "Số tiền giảm", "Số tiền tối thiểu", "Số tiền tối đa",
                    "Ngày bắt đầu", "Ngày kết thúc", "Mô tả", "Ngày Tạo", "Ngày Sửa", "Người Tạo", "Người Sửa"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 1;
            for (Voucher v : data) {
                Row row = sheet.createRow(rowIdx++);

                // 0..9
                createCell(row, 0, v.getId(), dataStyle);
                createCell(row, 1, v.getCustomer() != null ? v.getCustomer().getCustomerName() : "", dataStyle);
                createCell(row, 2, v.getEmployee() != null ? v.getEmployee().getEmployeeName() : "", dataStyle);
                createCell(row, 3, v.getVoucherCode(), dataStyle);
                createCell(row, 4, v.getVoucherName(), dataStyle);
                createCell(row, 5, v.getDiscountPercentage(), dataStyle);
                createCell(row, 6, v.getQuantity(), dataStyle);
                createCell(row, 7, v.getDiscountAmount(), dataStyle);
                createCell(row, 8, v.getMinOrderValue(), dataStyle);
                createCell(row, 9, v.getMaxDiscountValue(), dataStyle);

                // 10,11: ngày bắt đầu/kết thúc (ghi Date thật để áp dụng dateStyle)
                createDateCell(row, 10, v.getStartDate(), dateStyle);
                createDateCell(row, 11, v.getEndDate(), dateStyle);

                // 12: mô tả
                createCell(row, 12, v.getDescription(), dataStyle);

                // 13,14: ngày tạo/sửa
                createDateCell(row, 13, v.getCreatedDate(), dateStyle);
                createDateCell(row, 14, v.getUpdatedDate(), dateStyle);

                // 15,16: người tạo/sửa
                createCell(row, 15, v.getCreatedBy(), dataStyle);
                createCell(row, 16, v.getUpdatedBy(), dataStyle);
            }

            // Auto-size columns + min/max width
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3000) sheet.setColumnWidth(i, 3000);
                if (sheet.getColumnWidth(i) > 15000) sheet.setColumnWidth(i, 15000);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // ===== Styles =====
    private static CellStyle createHeaderStyle(Workbook workbook) {
        XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);

        style.setFillForegroundColor(new XSSFColor(new java.awt.Color(135, 206, 235), null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper ch = workbook.getCreationHelper();
        // Dùng định dạng Excel: tháng "MM", giờ "HH"
        style.setDataFormat(ch.createDataFormat().getFormat("dd/MM/yyyy HH:mm:ss"));
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    // ===== Helpers =====
    private static void createCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value != null ? value.toString() : "");
        }
        cell.setCellStyle(style);
    }

    private static void createDateCell(Row row, int column, Object dateVal, CellStyle dateStyle) {
        Cell cell = row.createCell(column);
        if (dateVal instanceof Date) {
            cell.setCellValue((Date) dateVal);
        } else if (dateVal == null) {
            cell.setBlank();
        } else {
            // fallback (khi entity để kiểu khác, ví dụ String), vẫn hiển thị
            cell.setCellValue(dateVal.toString());
        }
        cell.setCellStyle(dateStyle);
    }
}
