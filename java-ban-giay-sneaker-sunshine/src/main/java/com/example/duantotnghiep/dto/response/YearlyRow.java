// response/YearlyRow.java
package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor; import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter @AllArgsConstructor
public class YearlyRow {
    private Integer year;
    private BigDecimal totalRevenue;
    private Long totalQuantity;
}
