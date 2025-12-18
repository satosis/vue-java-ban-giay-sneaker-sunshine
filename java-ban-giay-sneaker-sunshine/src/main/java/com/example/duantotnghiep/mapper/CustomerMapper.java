package com.example.duantotnghiep.mapper;

import com.example.duantotnghiep.dto.request.CustomerRequest;
import com.example.duantotnghiep.dto.response.BadCustomerResponse;
import com.example.duantotnghiep.dto.response.CustomerResponse;
import com.example.duantotnghiep.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerResponse toDto(Customer customer);

    Customer toDto(CustomerRequest customerRequest);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "customerCode", target = "customerCode")
    @Mapping(source = "customerName", target = "customerName")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "phone", target = "phone")
    @Mapping(source = "isBlacklisted", target = "isBlacklisted")
    @Mapping(source = "blacklistReason", target = "blacklistReason")
    @Mapping(source = "blacklistExpiryDate", target = "blacklistExpiryDate")
    @Mapping(source = "trustScore", target = "trustScore")
    BadCustomerResponse toBadCustomerDto(Customer customer);

    List<BadCustomerResponse> toBadCustomerDtoList(List<Customer> customers);
}


