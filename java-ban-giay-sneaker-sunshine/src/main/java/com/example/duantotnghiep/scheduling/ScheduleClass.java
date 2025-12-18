package com.example.duantotnghiep.scheduling;

import com.example.duantotnghiep.repository.VoucherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class ScheduleClass {
    private static final Logger log = LoggerFactory.getLogger(ScheduleClass.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");

    private final VoucherRepository voucherRepository;

    public ScheduleClass(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    @Scheduled(initialDelay = 5_000, fixedRate = 10_000)
    @Transactional
    public void reconcileVoucherStatuses() {
        LocalDateTime now = LocalDateTime.now(ZONE);
        try {
            log.debug("Voucher scheduler running at {}", now);

            int activated = voucherRepository.activatePendingVouchers(now);
            if (activated > 0) {
                log.info("Activated {} voucher(s) (status 2 -> 1) at {}", activated, now);
            }

            int expired = voucherRepository.expirePendingVouchers(now);
            if (expired > 0) {
                log.info("Expired {} voucher(s) (status 2 -> 0) at {}", expired, now);
            }

            if (activated == 0 && expired == 0) {
                log.debug("No pending vouchers to update at {}", now);
            }
        } catch (Exception ex) {
            log.error("Error while reconciling voucher statuses at {}: {}", now, ex.getMessage(), ex);
        }
    }
}
