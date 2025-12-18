package com.example.duantotnghiep.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CustomerInfo {
    private Long customerId; // thêm trường này
    private String customerName;
    private String email;
    private String phone;
    private AddressRequest address;
}

