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
public class TongDonResponse {
    private Integer soLuongDon;
    private Integer soLuongSanPham;
    private BigDecimal tongTien;
}
