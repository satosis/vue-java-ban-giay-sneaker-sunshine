package com.example.duantotnghiep.mapper;

import com.example.duantotnghiep.dto.request.CategoryRequest;
import com.example.duantotnghiep.dto.response.CategoryResponse;
import com.example.duantotnghiep.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;


@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponse toResponse(Category category);


    @Mapping(target = "id", ignore = true)
    Category toEntity(CategoryRequest categoryRequest);

    // Ánh xạ từ request DTO sang entity (cập nhật)
    @Mapping(target = "id", ignore = true)
    void updateEntity(@MappingTarget Category category, CategoryRequest categoryRequest);
}
