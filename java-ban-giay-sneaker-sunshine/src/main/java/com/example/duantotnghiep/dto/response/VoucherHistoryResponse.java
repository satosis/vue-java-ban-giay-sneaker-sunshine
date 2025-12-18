package com.example.duantotnghiep.dto.response;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class VoucherHistoryResponse{
    Long id;
    @NotNull
    VoucherResponse voucher;
    LocalDateTime usedAt;
    BigDecimal discountValueApplied;
    Integer status;
    LocalDateTime createdDate;
    LocalDateTime updatedDate;
    @Size(max = 50)
    String createdBy;
    @Size(max = 50)
    String updatedBy;
}