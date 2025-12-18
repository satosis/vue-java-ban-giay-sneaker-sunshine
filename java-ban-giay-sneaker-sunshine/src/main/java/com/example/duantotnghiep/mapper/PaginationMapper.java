package com.example.duantotnghiep.mapper;

import com.example.duantotnghiep.dto.response.PaginationDTO;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface PaginationMapper {
    default <T> PaginationDTO<T> toPaginationDTO(Page<T> page) {
        return new PaginationDTO<>(
                page.getContent(),
                page.getNumber() + 1, // page number starts from 0, convert to 1-based
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
