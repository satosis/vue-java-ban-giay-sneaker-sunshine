package com.example.duantotnghiep.service;

import com.example.duantotnghiep.dto.request.DiscountCampaignRequest;
import com.example.duantotnghiep.dto.response.DiscountCampaignResponse;
import com.example.duantotnghiep.dto.response.DiscountCampaignStatisticsResponse;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface DiscountCampaignService {
    Page<DiscountCampaignResponse> getAll(int page, int size);

    DiscountCampaignResponse getDetail(Long id);

    @Transactional
    void delete(Long id);

    @Transactional
    DiscountCampaignResponse createDiscountCampaign(DiscountCampaignRequest request);

    @Transactional
    DiscountCampaignResponse updateDiscountCampaign(Long id, DiscountCampaignRequest request);

    DiscountCampaignStatisticsResponse getStatistics(Long campaignId);

    Page<DiscountCampaignResponse> search(String keyword,
                                          Integer status,
                                          LocalDate createdDate,
                                          Pageable pageable);
}
