package com.example.duantotnghiep.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductHistoryRequest {
    private Long productId;
    private String actionType;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private Long employeeId;
    private String note;
}
