package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.dto.response.CustomerBlacklistHistoryResponse;
import com.example.duantotnghiep.mapper.CustomerBlacklistHistoryMapper;
import com.example.duantotnghiep.model.CustomerBlacklistHistory;
import com.example.duantotnghiep.repository.CustomerBlacklistHistoryRepository;
import com.example.duantotnghiep.service.CustomerBlacklistHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerBlacklistHistoryServiceImpl implements CustomerBlacklistHistoryService {

    private final CustomerBlacklistHistoryRepository historyRepository;
    private final CustomerBlacklistHistoryMapper historyMapper;

    @Override
    public List<CustomerBlacklistHistoryResponse> getHistoryByCustomerId(Long customerId) {
        List<CustomerBlacklistHistory> histories = historyRepository.findByCustomerId(customerId);
        return historyMapper.toDtoList(histories);
    }

}

