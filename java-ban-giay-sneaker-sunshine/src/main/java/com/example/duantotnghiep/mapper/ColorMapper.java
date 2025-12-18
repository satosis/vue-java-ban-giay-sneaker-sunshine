package com.example.duantotnghiep.mapper;

import com.example.duantotnghiep.dto.response.ColorResponse;
import com.example.duantotnghiep.model.Color;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ColorMapper {
    ColorResponse toDto(Color color);

}
