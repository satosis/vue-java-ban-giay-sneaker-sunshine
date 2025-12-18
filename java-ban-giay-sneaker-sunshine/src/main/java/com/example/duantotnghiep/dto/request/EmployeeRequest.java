package com.example.duantotnghiep.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EmployeeRequest {
    private String employeeName;
    private String username;
    private String password;
    private String email;
    private String phone;
    private Integer gender;
    private Date dateOfBirth;
    private Date hireDate;
    private BigDecimal salary;

    private String country;
    private String province;
    private String district;
    private String ward;
    private String houseName;
    private String createdBy;
    private String updatedBy;
    private Integer role;

}
