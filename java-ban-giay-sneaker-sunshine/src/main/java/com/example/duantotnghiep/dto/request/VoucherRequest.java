package com.example.duantotnghiep.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class VoucherRequest {
    private Long customerId;
    private Long employeeId;
    private String voucherCode;
    private String voucherName;
    private BigDecimal discountPercentage;
    private Integer discountAmount;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountValue;
    private Integer quantity;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;
    private Integer status;
    private String description;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String createdBy;
    private String updatedBy;
    private Integer orderType;
    private Integer voucherType;
    private Long productId;
    private Long categoryId;

    private BigDecimal minOrderToReceive;

}
