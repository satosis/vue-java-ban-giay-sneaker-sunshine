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
public class AddressCustomerResponse {
    private Long id;
    private Long customerId;
    private String country;
    private String provinceCode;
    private String provinceName;
    private String districtCode;
    private String districtName;
    private String wardCode;
    private String wardName;
    private String houseName;
    private Integer status;
    private Boolean defaultAddress;
    private Date createdDate;
    private Date updatedDate;
    private String createdBy;
    private String updatedBy;
}
