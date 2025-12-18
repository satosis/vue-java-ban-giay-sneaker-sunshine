package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductHistoryResponse {
    private Long id;
    private Long productId;
    private String actionType;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private Long employeeId;
    private String employeeName;
    private Date createdDate;
    private String note;
}
