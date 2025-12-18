package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.request.EmployeeReportRequest;
import com.example.duantotnghiep.dto.request.StatisticFilterRequest;
import com.example.duantotnghiep.dto.request.StatisticsDashboardRequest;
import com.example.duantotnghiep.dto.response.*;
import com.example.duantotnghiep.service.InvoiceService;
import com.example.duantotnghiep.service.StatisticService;
import com.example.duantotnghiep.service.impl.StatisticExcelExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class StatisticController {

    private final StatisticService statisticService;
    private final StatisticExcelExportService excelService;


    @PostMapping("/dashboard")
    public ResponseEntity<StatisticsDashboardResponse> dashboard(@RequestBody StatisticsDashboardRequest req) {
        // Bảo toàn behavior đúng với FE (groupBy/metric/flags + startDate/endDate)
        StatisticsDashboardResponse data = statisticService.getDashboard(req);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/employee-sales")
    public ResponseEntity<List<EmployeeReportDto>> getEmployeeSalesReport(@RequestBody EmployeeReportRequest request) {
        List<EmployeeReportDto> reports = statisticService.getEmployeeSalesReport(request);
        return ResponseEntity.ok(reports);
    }

    @PostMapping("/dashboard/export-excel")
    public void exportDashboard(@RequestBody StatisticsDashboardRequest req, HttpServletResponse resp) throws IOException {
        excelService.exportDashboardExcel(resp, req);
    }

    @PostMapping("/employee/export-excel")
    public void exportEmployee(@RequestBody EmployeeReportRequest req, HttpServletResponse resp) throws IOException {
        excelService.exportEmployeeReportExcel(resp, req);
    }

}

