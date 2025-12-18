package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaginationDTO<T> {
    private List<T> data;
    private PaginationInfo pagination;

    public PaginationDTO(List<T> data, int currentPage, int pageSize, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious) {
        this.data = data;
        this.pagination = new PaginationInfo(currentPage, pageSize, totalElements, totalPages, hasNext, hasPrevious);
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class PaginationInfo {
        private int currentPage;
        private int pageSize;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }
}
