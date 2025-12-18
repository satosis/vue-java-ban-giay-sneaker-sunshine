// response/StatusStat.java
package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor; import lombok.Getter; import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class StatusStat {
    private Integer statusCode;   // code từ enum converter
    private String  status;       // tên hiển thị
    private Long    totalInvoices;
}
