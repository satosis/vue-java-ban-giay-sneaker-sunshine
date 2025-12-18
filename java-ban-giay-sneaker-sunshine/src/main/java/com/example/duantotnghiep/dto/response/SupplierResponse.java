package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SupplierResponse {
    private Long id;
    private String supplierCode;
    private String supplierName;
    private String country;
    private String province;
    private String district;
    private String ward;
    private String houseName;
    private Integer supplierStatus;
    private Date createdDate;
    private Date updatedDate;
    private String createdBy;
    private String updatedBy;
}
