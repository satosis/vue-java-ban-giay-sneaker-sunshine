package com.example.duantotnghiep.dto.request;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class VoucherSearchRequest {

    @Nullable
    private String keyword;

    @Nullable
    private Integer status;

    @Nullable
    private Integer orderType;

    @Nullable
    private Integer voucherType;

    @Nullable
    private Long categoryId;

    @Nullable
    private Long productId;
    @Nullable
    private Integer page;

    @Nullable
    private Integer size;
}
