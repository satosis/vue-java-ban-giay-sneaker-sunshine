package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EmployeeReportDto {
    private Long employeeId;
    private String employeeName;
    private Integer totalInvoices;
    private Integer totalProducts;
    private BigDecimal totalRevenue;
    private Integer successInvoices;
    private Integer successProducts;
    private BigDecimal successRevenue;
    private Integer cancelledInvoices;
    private Integer cancelledProducts;
    private BigDecimal cancelledRevenue;
}
