package com.example.duantotnghiep.service;

import com.example.duantotnghiep.dto.request.EmployeeRequest;
import com.example.duantotnghiep.dto.response.EmployeeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface EmployeeService {
    Page<EmployeeResponse> getAllUsers(Pageable pageable);

    EmployeeResponse getUserById(Long id);
    EmployeeResponse updateUser(Long id, EmployeeRequest employeeRequest);
    void deleteUser(Long id);
    EmployeeResponse createUser(EmployeeRequest employeeRequest);

    List<EmployeeResponse> getAllData();

    @Transactional(readOnly = true)
    List<EmployeeResponse> searchEmployees(String employeeCode, String employeeName, String email);
}
