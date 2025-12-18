// response/TopProductRow.java
package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor; import lombok.Getter; import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class TopProductRow {
    private Long   productId;
    private String productName;
    private Long   totalQuantitySold;
}
