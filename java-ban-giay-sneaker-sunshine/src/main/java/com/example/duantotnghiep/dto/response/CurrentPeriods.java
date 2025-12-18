// response/CurrentPeriods.java
package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor; import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter @AllArgsConstructor
public class CurrentPeriods {
    private BigDecimal weekRevenue;
    private BigDecimal prevWeekRevenue;
    private BigDecimal monthRevenue;
    private BigDecimal prevMonthRevenue;
}
