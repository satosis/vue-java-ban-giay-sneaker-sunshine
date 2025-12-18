package com.example.duantotnghiep.mapper;

import com.example.duantotnghiep.dto.response.CustomerBlacklistHistoryResponse;
import com.example.duantotnghiep.model.CustomerBlacklistHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomerBlacklistHistoryMapper {

    @Mapping(source = "customer.id", target = "customerId")
    CustomerBlacklistHistoryResponse toDto(CustomerBlacklistHistory history);

    List<CustomerBlacklistHistoryResponse> toDtoList(List<CustomerBlacklistHistory> histories);
}

