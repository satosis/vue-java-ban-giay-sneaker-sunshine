package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CustomerResponse {
    private Long id;
    private String customerCode;
    private String customerName;
    private String username;
    private String password;
    private String email;
    private String phone;
    private Integer gender;
    private Date dateOfBirth;
    private String country;
    private String provinceCode;
    private String provinceName;
    private String districtCode;
    private String districtName;
    private String wardCode;
    private String wardName;
    private String houseName;
    private String createdBy;
    private String updatedBy;
    private Integer role;
    private Integer status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    private Integer trustScore;
    private String blacklistReason;
    private Boolean isBlacklisted;
}