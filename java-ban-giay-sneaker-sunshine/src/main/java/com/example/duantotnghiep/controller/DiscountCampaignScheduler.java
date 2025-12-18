package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.model.DiscountCampaign;
import com.example.duantotnghiep.repository.DiscountCampaignRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DiscountCampaignScheduler {

    private final DiscountCampaignRepository repository;

    public DiscountCampaignScheduler(DiscountCampaignRepository repository) {
        this.repository = repository;
    }

    /**
     * Mỗi phút: kích hoạt các campaign đã đến giờ chạy
     * Điều kiện: startDate <= now AND status = 0 (CHỜ BẮT ĐẦU) -> set status = 1 (ĐANG CHẠY)
     */
    @Scheduled(cron = "*/10 * * * * *") // mỗi 10 giây
    @Transactional
    public void autoActivateScheduledCampaigns() {
        LocalDateTime now = LocalDateTime.now();
        List<DiscountCampaign> toActivate = repository.findAllByStartDateLessThanEqualAndStatus(now, 0);
        if (toActivate.isEmpty()) return;

        for (DiscountCampaign campaign : toActivate) {
            campaign.setStatus(1); // ĐANG CHẠY
            campaign.setUpdatedDate(now);
        }
        repository.saveAll(toActivate);
    }

    /**
     * Mỗi phút: vô hiệu các campaign đã hết hạn
     * Điều kiện: endDate < now AND status != 2 -> set status = 2 (VÔ HIỆU/HẾT HẠN)
     */
    @Scheduled(cron = "*/10 * * * * *") // mỗi 10 giây
    @Transactional
    public void autoDeactivateExpiredCampaigns() {
        LocalDateTime now = LocalDateTime.now();
        List<DiscountCampaign> expiredCampaigns = repository.findAllByEndDateBeforeAndStatusNot(now, 2);
        if (expiredCampaigns.isEmpty()) return;

        for (DiscountCampaign campaign : expiredCampaigns) {
            campaign.setStatus(2); // VÔ HIỆU/HẾT HẠN
            campaign.setUpdatedDate(now);
        }
        repository.saveAll(expiredCampaigns);
    }
}




