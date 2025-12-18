package com.example.duantotnghiep.mapper;

import com.example.duantotnghiep.dto.response.ProductImageResponse;
import com.example.duantotnghiep.model.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductImageMapper {

    @Mapping(target = "colorId", source = "color.id")
    @Mapping(target = "colorName", source = "color.colorName")
    ProductImageResponse toResponse(ProductImage productImage);

    List<ProductImageResponse> toResponseList(List<ProductImage> productImages);
}
