package com.example.duantotnghiep.mapper;

import com.example.duantotnghiep.dto.request.AddressCustomerRequest;
import com.example.duantotnghiep.dto.request.CustomerRequest;
import com.example.duantotnghiep.dto.response.AddressCustomerResponse;
import com.example.duantotnghiep.model.AddressCustomer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AddressMapper {
    AddressCustomer toAddressCustomerEntitty(CustomerRequest request);

    @Mapping(source = "customer.id", target = "customerId")
    AddressCustomerResponse toResponse(AddressCustomer addressCustomer);

    @Mapping(target = "customer.id", source = "customerId")
    @Mapping(target = "id", ignore = true)
    AddressCustomer toEntity(AddressCustomerRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer.id", source = "customerId")
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntityFromRequest(AddressCustomerRequest request, @MappingTarget AddressCustomer entity);

}

