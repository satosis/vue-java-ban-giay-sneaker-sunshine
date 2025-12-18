package com.example.duantotnghiep.Exception;

import java.io.Serial;
import java.util.List;

public class DiscountCampaignInvalidException extends RuntimeException {
    @Serial private static final long serialVersionUID = 1L;

    private final String code;
    private final List<String> productNames; // sp bị ảnh hưởng (tuỳ chọn)

    public DiscountCampaignInvalidException(String message) {
        super(message);
        this.code = "CAMPAIGN_REMOVED";
        this.productNames = null;
    }

    public DiscountCampaignInvalidException(String code, String message, List<String> productNames) {
        super(message);
        this.code = (code == null || code.isBlank()) ? "CAMPAIGN_REMOVED" : code;
        this.productNames = productNames;
    }

    public static DiscountCampaignInvalidException removed(List<String> productNames) {
        return new DiscountCampaignInvalidException(
                "CAMPAIGN_REMOVED",
                "Một số sản phẩm đã hết giảm giá do đợt khuyến mãi bị vô hiệu hoá hoặc hết hạn. Giá đã quay về giá gốc.",
                productNames
        );
    }

    public String getCode() { return code; }
    public List<String> getProductNames() { return productNames; }
}

