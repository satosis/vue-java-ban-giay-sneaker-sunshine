package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class OrderStatusHistoryResponse {
    private Long id;
    private Long invoiceId;
    private Long employeeId;
    private String employeeName;
    private Integer oldStatus;
    private Integer newStatus;
    private Date changedAt;
    private String note;

}
