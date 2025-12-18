package com.example.duantotnghiep.mapper;

import com.example.duantotnghiep.dto.request.ProductHistoryRequest;
import com.example.duantotnghiep.dto.response.ProductHistoryResponse;
import com.example.duantotnghiep.model.ProductHistory;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductHistoryMapper {
    ProductHistoryResponse toDto(ProductHistory entity);

    ProductHistory toEntity(ProductHistoryRequest dto);

    List<ProductHistoryResponse> toResponseList(List<ProductHistory> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(ProductHistoryRequest request, @MappingTarget ProductHistory entity);
}
