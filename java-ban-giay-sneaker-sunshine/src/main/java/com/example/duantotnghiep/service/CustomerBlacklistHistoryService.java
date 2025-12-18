package com.example.duantotnghiep.service;

import com.example.duantotnghiep.dto.response.CustomerBlacklistHistoryResponse;

import java.util.List;

public interface CustomerBlacklistHistoryService {
    List<CustomerBlacklistHistoryResponse> getHistoryByCustomerId(Long customerId);
}
