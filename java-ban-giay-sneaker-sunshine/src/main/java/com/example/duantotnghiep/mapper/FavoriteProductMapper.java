package com.example.duantotnghiep.mapper;

import com.example.duantotnghiep.dto.request.FavoriteProductRequest;
import com.example.duantotnghiep.dto.response.FavoriteProductResponse;
import com.example.duantotnghiep.model.FavoriteProduct;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FavoriteProductMapper {
    FavoriteProduct toEntity(FavoriteProductRequest request);
    FavoriteProductResponse toDto(FavoriteProduct entity);
}
