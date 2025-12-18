package com.example.duantotnghiep.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class VerifyPricesResponse {
    private boolean ok;
    private String message; // nếu ok=false -> "Giá đã được cập nhật..."
    private List<DiffItem> diffs = new ArrayList<>();

    @Getter @Setter
    public static class DiffItem {
        private Long productDetailId;
        private Integer quantity;

        // FE snapshot
        private BigDecimal feSellPrice;
        private BigDecimal feDiscountedPrice;
        private Integer fePercent;
        private Long feCampaignId;

        // Server current
        private BigDecimal svSellPrice;
        private BigDecimal svDiscountedPrice;
        private Integer svPercent;
        private Long svCampaignId;

        private String reason; // mô tả ngắn phần lệch
    }
}

