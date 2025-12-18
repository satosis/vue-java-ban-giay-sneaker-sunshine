package com.example.duantotnghiep.mapper;

import com.example.duantotnghiep.dto.request.EmployeeRequest;
import com.example.duantotnghiep.dto.response.EmployeeResponse;
import com.example.duantotnghiep.model.Employee;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {


    EmployeeResponse toEmployeeResponse(Employee employee);

    Employee toEntity(EmployeeRequest employeeRequest);
    List<EmployeeResponse> toEmployeeResponseList(List<Employee> employees);

    List<Employee> toEntityList(List<EmployeeRequest> employeeRequests);
}
