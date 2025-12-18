package com.example.duantotnghiep.mapper;

import com.example.duantotnghiep.dto.request.CustomerRequest;
import com.example.duantotnghiep.dto.request.EmployeeRequest;
import com.example.duantotnghiep.dto.response.CustomerResponse;
import com.example.duantotnghiep.dto.response.EmployeeResponse;
import com.example.duantotnghiep.dto.response.UserDTO;
import com.example.duantotnghiep.model.Customer;
import com.example.duantotnghiep.model.Employee;
import com.example.duantotnghiep.model.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "employeeName", expression = "java(user.getEmployee() != null ? user.getEmployee().getEmployeeName() : null)")
    @Mapping(target = "customerName", expression = "java(user.getCustomer() != null ? user.getCustomer().getCustomerName() : null)")
    @Mapping(target = "customerId", expression = "java(user.getCustomer() != null ? user.getCustomer().getId() : null)") // 👈 THÊM DÒNG NÀY
    UserDTO toDto(User user);

    @Mapping(target = "username", source = "username")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "id", source = "employee.id")
    @Mapping(target = "employeeCode", source = "employee.employeeCode")
    @Mapping(target = "employeeName", source = "employee.employeeName")
    @Mapping(target = "email", source = "employee.email")
    @Mapping(target = "phone", source = "employee.phone")
    @Mapping(target = "gender", source = "employee.gender")
    @Mapping(target = "dateOfBirth", source = "employee.dateOfBirth")
    @Mapping(target = "hireDate", source = "employee.hireDate")
    @Mapping(target = "salary", source = "employee.salary")
    @Mapping(target = "country", source = "employee.country")
    @Mapping(target = "province", source = "employee.province")
    @Mapping(target = "district", source = "employee.district")
    @Mapping(target = "ward", source = "employee.ward")
    @Mapping(target = "houseName", source = "employee.houseName")
    @Mapping(target = "createdBy", source = "employee.createdBy")
    @Mapping(target = "status", source = "employee.status")
    @Mapping(target = "createdDate", source = "employee.createdDate")
    @Mapping(target = "updatedBy", source = "employee.updatedBy")
    @Mapping(target = "updatedDate", source = "employee.updatedDate")
    EmployeeResponse toEmployeeResponse(User user);

    @Mapping(target = "username", source = "username")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "id", source = "customer.id")
    @Mapping(target = "customerCode", source = "customer.customerCode")
    @Mapping(target = "customerName", source = "customer.customerName")
    @Mapping(target = "email", source = "customer.email")
    @Mapping(target = "phone", source = "customer.phone")
    @Mapping(target = "gender", source = "customer.gender")
    @Mapping(target = "dateOfBirth", source = "customer.dateOfBirth")
    @Mapping(target = "createdBy", source = "customer.createdBy")
    @Mapping(target = "createdDate", source = "customer.createdDate")
    @Mapping(target = "updatedBy", source = "customer.updatedBy")
    @Mapping(target = "status", source = "customer.status")
    @Mapping(target = "updatedDate", source = "customer.updatedDate")
    CustomerResponse toCustomerResponse(User user);

    Customer toCustomerEntity(CustomerRequest request);
    User toCustomerUserEntity(CustomerRequest customerRequest);

    Employee toEmployeeEntity(EmployeeRequest request);

    User toUserEntity(EmployeeRequest employeeRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEmployeeFromRequest(EmployeeRequest request, @MappingTarget Employee employee);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCustomerFromRequest(CustomerRequest request, @MappingTarget Customer customer);
}

