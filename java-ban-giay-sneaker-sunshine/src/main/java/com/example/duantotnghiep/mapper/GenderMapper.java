package com.example.duantotnghiep.mapper;

import com.example.duantotnghiep.dto.response.GenderDTO;
import com.example.duantotnghiep.model.Gender;

import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GenderMapper {
    GenderDTO toDTO(Gender entity);
    List<GenderDTO> toDTOs(List<Gender> entities);

}
