package com.example.duantotnghiep.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BadCustomerResponse {
    private Long id;
    private String customerCode;
    private String customerName;
    private String email;
    private String phone;
    private Boolean isBlacklisted;
    private String blacklistReason;
    private LocalDateTime blacklistExpiryDate;
    private Integer trustScore;
}

