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
public class VoucherStatusDTO {
    private Long voucherId;            // id của voucher
    private String voucherCode;        // mã voucher
    private String voucherName;        // tên voucher
    private BigDecimal totalDiscountGiven; // tổng số tiền giảm giá đã áp dụng
    private BigDecimal discountToday;      // tổng số tiền giảm trong ngày
    private Long totalUses;                // tổng số lần đã sử dụng
    private Long usesToday;                // số lần dùng trong ngày
}
