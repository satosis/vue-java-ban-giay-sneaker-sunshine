// response/MonthlyRow.java
package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor; import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter @AllArgsConstructor
public class MonthlyRow {
    private Integer year;
    private Integer month;
    private BigDecimal totalRevenue;
    private Long totalQuantity;
}
