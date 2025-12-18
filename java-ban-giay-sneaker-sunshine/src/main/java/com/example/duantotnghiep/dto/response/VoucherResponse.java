package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Data
public class VoucherResponse {
    private Long id;
    private String voucherCode;
    private String voucherName;
    private BigDecimal discountPercentage;
    private Integer discountAmount;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountValue;
    private Integer quantity;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer status;
    private String description;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String createdBy;
    private String updatedBy;
    private Integer orderType;
    private Integer voucherType;
    private String customerName;
    private String customerId;
    private String employeeName;
    private String employeeId;
    private String productName;
    private String productId;
    private String categoryName;
    private String categoryId;
    private BigDecimal minOrderToReceive; // ✅ đảm bảo có trường này
}