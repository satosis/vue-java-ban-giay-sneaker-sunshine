package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.request.EmployeeRequest;
import com.example.duantotnghiep.dto.response.EmployeeResponse;
import com.example.duantotnghiep.service.EmployeeService;
import com.example.duantotnghiep.xuatExcel.EmployeeExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;
    private final EmployeeExportService employeeExportService;

    //  Get all employees with pagination
    @GetMapping
    public Page<EmployeeResponse> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return employeeService.getAllUsers(pageable);
    }

    // Get single employee by ID
    @GetMapping("/{id}")
    public EmployeeResponse getEmployeeById(@PathVariable Long id) {
        return employeeService.getUserById(id);
    }

    // Create new employee
    @PostMapping
    public EmployeeResponse createEmployee(@RequestBody EmployeeRequest request) {
        System.out.println("pass:" +request.getPassword());
        return employeeService.createUser(request);
    }

    // Update employee by ID
    @PutMapping("/{id}")
    public EmployeeResponse updateEmployee(@PathVariable Long id, @RequestBody EmployeeRequest request) {
        return employeeService.updateUser(id, request);
    }

    //  Soft delete employee by ID
    @DeleteMapping("/{id}")
    public void deleteEmployee(@PathVariable Long id) {
        employeeService.deleteUser(id);
    }

    @GetMapping("/get-data")
    public List<EmployeeResponse> getEmployeeData() {
        return employeeService.getAllData();
    }

    @GetMapping("/search")
    public ResponseEntity<List<EmployeeResponse>> searchEmployees(
            @RequestParam(required = false) String employeeCode,
            @RequestParam(required = false) String employeeName,
            @RequestParam(required = false) String email
    ) {
        List<EmployeeResponse> result = employeeService.searchEmployees(employeeCode, employeeName, email);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportEmployeesExcel(
            @RequestParam(required = false) String employeeCode,
            @RequestParam(required = false) String employeeName,
            @RequestParam(required = false) String email
    ) {
        byte[] file = employeeExportService.exportEmployeesExcel(employeeCode, employeeName, email);

        String filename = "employees-" + java.time.LocalDateTime.now()
                .toString().replace(":", "-")
                + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(file.length)
                .body(file);
    }
}
