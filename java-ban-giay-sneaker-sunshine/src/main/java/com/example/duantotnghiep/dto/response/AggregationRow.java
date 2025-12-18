// response/AggregationRow.java
package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor; import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter @AllArgsConstructor
public class AggregationRow {
    private String label;           // ngày (YYYY-MM-DD) / "MM/YYYY" / "YYYY"
    private BigDecimal totalRevenue;
    private Long totalQuantity;
}
