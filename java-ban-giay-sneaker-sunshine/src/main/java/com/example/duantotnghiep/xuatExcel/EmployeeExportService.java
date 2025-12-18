package com.example.duantotnghiep.xuatExcel;

import com.example.duantotnghiep.model.Employee;
import com.example.duantotnghiep.repository.EmployeeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeExportService {

    private final EmployeeRepository employeeRepository;

    @Transactional
    public byte[] exportEmployeesExcel(String employeeCode, String employeeName, String email) {
        // Lấy dữ liệu (dùng query search như bạn đã có)
        List<Employee> employees = employeeRepository.searchEmployees(employeeCode, employeeName, email);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Employees");

            // ===== Styles =====
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle bodyStyle = wb.createCellStyle();
            bodyStyle.setBorderBottom(BorderStyle.THIN);
            bodyStyle.setBorderTop(BorderStyle.THIN);
            bodyStyle.setBorderLeft(BorderStyle.THIN);
            bodyStyle.setBorderRight(BorderStyle.THIN);
            bodyStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.cloneStyleFrom(bodyStyle);
            CreationHelper ch = wb.getCreationHelper();
            short df = ch.createDataFormat().getFormat("dd/MM/yyyy");
            dateStyle.setDataFormat(df);

            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.cloneStyleFrom(bodyStyle);
            moneyStyle.setDataFormat(ch.createDataFormat().getFormat("#,##0"));

            // ===== Header =====
            String[] headers = {
                    "ID", "Mã NV", "Họ tên", "Username", "Email", "SĐT",
                    "Giới tính", "Ngày sinh", "Ngày vào làm", "Lương",
                    "Quốc gia", "Tỉnh/TP", "Quận/Huyện", "Phường/Xã",
                    "Số nhà/ĐC", "Vai trò", "Trạng thái",
                    "Người tạo", "Ngày tạo", "Người sửa", "Ngày sửa"
            };

            Row header = sheet.createRow(0);
            header.setHeightInPoints(20);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // ===== Body =====
            int r = 1;
            for (Employee e : employees) {
                Row row = sheet.createRow(r++);

                int c = 0;
                setCell(row, c++, e.getId(), bodyStyle);
                setCell(row, c++, e.getEmployeeCode(), bodyStyle);
                setCell(row, c++, e.getEmployeeName(), bodyStyle);
                setCell(row, c++, e.getEmail(), bodyStyle);
                setCell(row, c++, e.getPhone(), bodyStyle);
                setCell(row, c++, genderName(e.getGender()), bodyStyle);
                setCell(row, c++, e.getDateOfBirth(), dateStyle);
                setCell(row, c++, e.getHireDate(), dateStyle);
                setCellNumeric(row, c++, e.getSalary(), moneyStyle);
                setCell(row, c++, e.getCountry(), bodyStyle);
                setCell(row, c++, e.getProvince(), bodyStyle);
                setCell(row, c++, e.getDistrict(), bodyStyle);
                setCell(row, c++, e.getWard(), bodyStyle);
                setCell(row, c++, e.getHouseName(), bodyStyle);
                setCell(row, c++, statusName(e.getStatus()), bodyStyle);
                setCell(row, c++, e.getCreatedBy(), bodyStyle);
                setCell(row, c++, e.getCreatedDate(), dateStyle);
                setCell(row, c++, e.getUpdatedBy(), bodyStyle);
                setCell(row, c++, e.getUpdatedDate(), dateStyle);
            }

            // Auto-size cột
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int width = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(width + 512, 10000)); // giới hạn max
            }

            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Xuất Excel thất bại", ex);
        }
    }

    // ===== Helpers =====
    private static void setCell(Row row, int idx, Object val, CellStyle style) {
        Cell cell = row.createCell(idx);
        if (val == null) {
            cell.setBlank();
        } else if (val instanceof Date d) {
            cell.setCellValue(d);
        } else {
            cell.setCellValue(String.valueOf(val));
        }
        cell.setCellStyle(style);
    }

    private static void setCellNumeric(Row row, int idx, BigDecimal val, CellStyle style) {
        Cell cell = row.createCell(idx);
        if (val == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(val.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private static String genderName(Integer g) {
        if (g == null) return "";
        return switch (g) {
            case 0 -> "Nữ";
            case 1 -> "Nam";
            default -> "Khác";
        };
    }

    private static String roleName(Integer r) {
        if (r == null) return "";
        return switch (r) {
            case 1 -> "ADMIN";
            case 2 -> "STAFF";
            case 3 -> "INTERN";
            default -> "UNKNOWN";
        };
    }

    private static String statusName(Integer s) {
        if (s == null) return "";
        return s == 1 ? "Hoạt động" : "Ngừng";
    }
}

