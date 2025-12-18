package com.example.duantotnghiep.mapper;

import com.example.duantotnghiep.dto.request.VoucherRequest;
import com.example.duantotnghiep.dto.response.VoucherResponse;
import com.example.duantotnghiep.model.Voucher;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface VoucherMapper {
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "category", ignore = true)
    Voucher toEntity(VoucherRequest dto);

    @Mapping(source = "customer.customerName", target = "customerName")
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "employee.employeeName", target = "employeeName")
    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "product.productName", target = "productName")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "category.categoryName", target = "categoryName")
    @Mapping(source = "category.id", target = "categoryId")
    VoucherResponse toDto(Voucher entity);

    // Optional for update
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "customer.id", source = "customerId")
    @Mapping(target = "employee.id", source = "employeeId")
    @Mapping(target = "product.id", source = "productId")
    @Mapping(target = "category.id", source = "categoryId")
    void updateVoucherFromDto(VoucherRequest dto, @MappingTarget Voucher entity);
}
