package com.example.duantotnghiep.mapper;

import com.example.duantotnghiep.dto.response.SizeResponse;
import com.example.duantotnghiep.model.Size;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SizeMapper {
    SizeResponse toDto(Size size);
}
