package com.example.duantotnghiep.dto.response;

public interface TimeAggRow {
    String getLabel();        // ví dụ "2025-08-31" hoặc "08/2025" hoặc "2025"
    Long getTotalRevenue();
    Long getTotalQuantity();
}
