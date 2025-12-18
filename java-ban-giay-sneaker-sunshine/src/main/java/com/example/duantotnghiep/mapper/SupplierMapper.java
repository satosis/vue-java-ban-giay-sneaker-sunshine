package com.example.duantotnghiep.mapper;

import com.example.duantotnghiep.dto.request.SupplierRequest;
import com.example.duantotnghiep.dto.response.SupplierResponse;
import com.example.duantotnghiep.model.Supplier;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SupplierMapper {
    Supplier toEntity(SupplierRequest request);

    SupplierResponse toResponse(Supplier supplier);
}
