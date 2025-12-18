package com.example.duantotnghiep.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CustomerBlacklistHistoryResponse {
    private Long id;
    private Long customerId;
    private String reason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

