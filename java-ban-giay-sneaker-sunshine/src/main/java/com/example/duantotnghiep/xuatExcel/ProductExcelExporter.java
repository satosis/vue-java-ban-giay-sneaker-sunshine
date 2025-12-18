package com.example.duantotnghiep.xuatExcel;

import com.example.duantotnghiep.dto.response.ProductSearchResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class ProductExcelExporter {
    public static ByteArrayInputStream exportProductToExcel(List<ProductSearchResponse> data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Products");

            // Create styles for header and data cells
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // Create header row
            String[] headers = {"ID","Danh mục" ,"Tên Sản Phẩm", "Mã Sản Phẩm", "Chất Liệu", "Thương Hiệu", "Cổ Giày", "Giới Tính", "Đế Giày","Cân nặng","Giá Bán","Số lượng","Mô tả","Ngày Tạo","Ngày Sửa","Người Tạo","Người Sửa"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            int rowIdx = 1;
            for (ProductSearchResponse product : data) {
                Row row = sheet.createRow(rowIdx++);
                createCell(row, 0, product.getId(), dataStyle);
                createCell(row, 1, product.getCategoryNames(), dataStyle);
                createCell(row, 2, product.getProductName(), dataStyle);
                createCell(row, 3, product.getProductCode(), dataStyle);
                createCell(row, 4, product.getMaterialName(), dataStyle);
                createCell(row, 5, product.getBrandName(), dataStyle);
                createCell(row, 6, product.getStyleName(), dataStyle);
                createCell(row, 7, product.getGenderName(), dataStyle);
                createCell(row, 8, product.getSoleName(), dataStyle);
                createCell(row, 9, product.getWeight(), dataStyle);
                createCell(row, 10, product.getSellPrice(), dataStyle);
                createCell(row, 11, product.getQuantity(), dataStyle);
                createCell(row, 12, product.getDescription(), dataStyle);
                createCell(row, 13, product.getCreatedDate() != null ? new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(product.getCreatedDate()) : "", dateStyle);
                createCell(row, 14, product.getUpdatedDate() != null ? new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(product.getUpdatedDate()) : "", dateStyle);
                createCell(row, 15, product.getCreatedBy(), dataStyle);
                createCell(row, 16, product.getUpdatedBy(), dataStyle);


            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // Set a minimum width to ensure readability
                if (sheet.getColumnWidth(i) < 3000) {
                    sheet.setColumnWidth(i, 3000);
                }
                // Set a maximum width to prevent overly wide columns
                if (sheet.getColumnWidth(i) > 15000) {
                    sheet.setColumnWidth(i, 15000);
                }
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // Helper method to create header style
    private static CellStyle createHeaderStyle(Workbook workbook) {
        XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);

        // Set light blue background using XSSFColor
        style.setFillForegroundColor(new XSSFColor(new java.awt.Color(135, 206, 235), null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Set borders
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // Center align
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    // Helper method to create data cell style
    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // Set borders
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // Center align
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    // Helper method to create date cell style
    private static CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy hh:mm:ss"));

        // Set borders
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // Center align
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    // Helper method to create and style cells
    private static void createCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value != null ? value.toString() : "Unknown");
        }
        cell.setCellStyle(style);
    }
}
