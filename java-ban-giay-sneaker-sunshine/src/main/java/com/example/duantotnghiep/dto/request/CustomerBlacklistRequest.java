package com.example.duantotnghiep.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerBlacklistRequest {
    private String reason;
    private int durationInDays; // số ngày cấm
}
