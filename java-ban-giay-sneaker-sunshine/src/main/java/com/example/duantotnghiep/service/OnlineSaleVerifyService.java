package com.example.duantotnghiep.service;

import com.example.duantotnghiep.dto.request.CartItemRequest;
import com.example.duantotnghiep.dto.request.InvoiceRequest;
import com.example.duantotnghiep.dto.response.VerifyPricesResponse;
import com.example.duantotnghiep.model.DiscountCampaign;
import com.example.duantotnghiep.model.DiscountCampaignProductDetail;
import com.example.duantotnghiep.model.Product;
import com.example.duantotnghiep.model.ProductDetail;
import com.example.duantotnghiep.repository.DiscountCampaignRepository;
import com.example.duantotnghiep.repository.ProductDetailRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * VERIFY-PRICES:
 * - Không throw exception (trừ lỗi hệ thống DB), luôn trả VerifyPricesResponse với ok=true/false + diffs.
 * - Chỉ áp khuyến mãi khi campaign đang active và có link SPCT hoặc Product.
 * - Hỗ trợ FE ép campaignId nếu campaign đó có link hợp lệ; nếu không hợp lệ -> bỏ qua ép, rơi về auto-pick tốt nhất.
 * - So sánh GIÁ SAU GIẢM với sai số ±1₫.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineSaleVerifyService {

    private final ProductDetailRepository productDetailRepository;
    private final DiscountCampaignRepository discountCampaignRepository;

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final long PRICE_TOLERANCE_VND = 1L; // ±1₫

    // ======================== Helpers ========================
    private static BigDecimal vnd(BigDecimal x) {
        if (x == null) return BigDecimal.ZERO;
        return x.setScale(0, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal x) {
        return x == null ? BigDecimal.ZERO : x;
    }

    private static boolean eqWithinVND(BigDecimal a, BigDecimal b, long tol) {
        if (a == null || b == null) return Objects.equals(a, b);
        return a.subtract(b).abs().compareTo(BigDecimal.valueOf(tol)) <= 0;
    }

    private static int calcPercentInt(BigDecimal sell, BigDecimal disc) {
        if (sell == null || sell.signum() == 0 || disc == null) return 0;
        return sell.subtract(disc)
                .multiply(ONE_HUNDRED)
                .divide(sell, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private static Long toLongLenient(Object v) {
        if (v == null) return null;
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s0) {
            String s = s0.trim();
            if (s.isEmpty()) return null;
            String digits = s.replaceAll("[^\\d-]", "");
            if (digits.isEmpty() || "-".equals(digits)) return null;
            try {
                return Long.valueOf(digits);
            } catch (Exception ignore) {
                return null;
            }
        }
        return null;
    }

    private static Integer toIntLenient(Object v, Integer def) {
        if (v == null) return def;
        if (v instanceof Integer i) return i;
        if (v instanceof Long l) return l.intValue();
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s0) {
            String s = s0.trim();
            if (s.isEmpty()) return def;
            String digits = s.replaceAll("[^\\d-]", "");
            if (digits.isEmpty() || "-".equals(digits)) return def;
            try {
                return Integer.valueOf(digits);
            } catch (Exception ignore) {
                return def;
            }
        }
        return def;
    }

    // ======================== Public API ========================
    @Transactional(readOnly = true)
    public VerifyPricesResponse verifyPrices(InvoiceRequest request) {
        VerifyPricesResponse resp = new VerifyPricesResponse();
        resp.setOk(true);

        // 0) Validate mức tối thiểu
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            resp.setOk(false);
            resp.setMessage("Không có sản phẩm để xác thực giá.");
            resp.setDiffs(new ArrayList<>());
            return resp;
        }

        // 1) Lấy campaign đang active (nếu repo trả null -> list rỗng)
        List<DiscountCampaign> activeCampaigns =
                Optional.ofNullable(discountCampaignRepository.findActiveCampaigns(LocalDateTime.now()))
                        .orElseGet(Collections::emptyList);

        List<VerifyPricesResponse.DiffItem> diffs = new ArrayList<>();
        String firstMismatchProductName = null;

        // 2) Duyệt từng item và so sánh giá
        for (CartItemRequest rawItem : request.getItems()) {
            if (rawItem == null) continue;

            Long productDetailId = toLongLenient(rawItem.getProductDetailId());
            Long forceCampaignId = toLongLenient(rawItem.getDiscountCampaignId());
            Integer quantity = Math.max(1, toIntLenient(rawItem.getQuantity(), 1));
            BigDecimal feSell = vnd(nz(rawItem.getSellPrice()));
            BigDecimal feDisc = rawItem.getDiscountedPrice() == null ? feSell : vnd(nz(rawItem.getDiscountedPrice()));
            String feName = Optional.ofNullable(rawItem.getProductName()).filter(s -> !s.isBlank()).orElse(null);

            // 2.1) Thiếu PD ID -> diff
            if (productDetailId == null) {
                diffs.add(buildDiff(null, quantity, feSell, feDisc, forceCampaignId,
                        null, null, null, null, "INVALID_PRODUCT_DETAIL_ID"));
                resp.setOk(false);
                if (firstMismatchProductName == null) firstMismatchProductName = feName != null ? feName : "một sản phẩm";
                continue;
            }

            // 2.2) Load PD & Product an toàn
            Optional<ProductDetail> pdOpt;
            try {
                pdOpt = productDetailRepository.findByIdAndStatus(productDetailId);
            } catch (Exception e) {
                pdOpt = Optional.empty();
            }

            if (pdOpt.isEmpty()) {
                diffs.add(buildDiff(productDetailId, quantity, feSell, feDisc, forceCampaignId,
                        null, null, null, null, "PRODUCT_DETAIL_NOT_FOUND_OR_INACTIVE"));
                resp.setOk(false);
                if (firstMismatchProductName == null) firstMismatchProductName = feName != null ? feName : ("SP " + productDetailId);
                continue;
            }

            ProductDetail pd = pdOpt.get();
            Product product = pd.getProduct();

            // 2.3) Tính giá server (chỉ giảm nếu có link PD/Product)
            Pricing sv = computePricingOnlyIfLinked(pd, product, activeCampaigns, forceCampaignId);

            // 2.4) So sánh GIÁ SAU GIẢM
            BigDecimal svDisc = sv.getDiscountedPrice();
            boolean mismatch = !eqWithinVND(feDisc, svDisc, PRICE_TOLERANCE_VND);
            if (mismatch) {
                resp.setOk(false);

                String productName =
                        (feName != null) ? feName
                                : (product != null && product.getProductName() != null
                                ? product.getProductName()
                                : ("SP " + pd.getId()));

                if (firstMismatchProductName == null) {
                    firstMismatchProductName = productName;
                }

                diffs.add(buildDiff(
                        productDetailId,
                        quantity,
                        feSell,
                        feDisc,
                        forceCampaignId,
                        sv.getSellPrice(),
                        sv.getDiscountedPrice(),
                        sv.getPercentInt(),
                        sv.getCampaignId(),
                        "DISCOUNTED_PRICE_MISMATCH"
                ));
            }
        }

        // 3) Kết luận
        if (!resp.isOk()) {
            String name = (firstMismatchProductName == null ? "một sản phẩm" : firstMismatchProductName);
            resp.setMessage("Một số sản phẩm không hợp lệ hoặc giá đã thay đổi: " + name + ".");
        } else {
            resp.setMessage("Xác thực giá thành công.");
        }
        resp.setDiffs(diffs);
        return resp;
    }

    // ======================== Core Pricing (liên kết thật) ========================
    private Pricing computePricingOnlyIfLinked(ProductDetail pd,
                                               Product product,
                                               List<DiscountCampaign> activeCampaigns,
                                               Long forceCampaignId) {

        // sellPrice ưu tiên ở PD, fallback Product, cuối cùng 0
        BigDecimal sellPrice = Optional.ofNullable(pd)
                .map(ProductDetail::getSellPrice)
                .orElseGet(() -> Optional.ofNullable(product)
                        .map(Product::getSellPrice)
                        .orElse(BigDecimal.ZERO));
        sellPrice = vnd(sellPrice);

        // 1) FE ép campaign
        if (forceCampaignId != null && activeCampaigns != null && !activeCampaigns.isEmpty()) {
            for (DiscountCampaign c : activeCampaigns) {
                if (c == null || c.getId() == null) continue;
                if (!Objects.equals(c.getId(), forceCampaignId)) continue;

                boolean linkPd = hasProductDetailInCampaignSafe(c, pd);
                boolean linkPrd = hasProductInCampaignSafe(c, product);

                if (linkPd || linkPrd) {
                    PctPick pick = pickPctWithLinks(c, pd, linkPd, linkPrd);
                    BigDecimal discounted = (pick.pct.compareTo(BigDecimal.ZERO) > 0)
                            ? sellPrice.multiply(ONE_HUNDRED.subtract(pick.pct)).divide(ONE_HUNDRED, 0, RoundingMode.HALF_UP)
                            : sellPrice;

                    Pricing p = new Pricing();
                    p.setCampaignId(c.getId());
                    p.setSellPrice(sellPrice);
                    p.setPercent(pick.pct);
                    p.setPercentInt(pick.pct.setScale(0, RoundingMode.HALF_UP).intValue());
                    p.setDiscountedPrice(vnd(discounted));
                    p.setHasPdInCampaign(linkPd);
                    p.setHasProductInCampaign(linkPrd);
                    return p;
                }
                // nếu ép campaign nhưng không có link -> bỏ qua, rơi xuống auto
            }
        }

        // 2) Auto: duyệt các campaign đang active có link, chọn % lớn nhất (tie-break ưu tiên link PD)
        BigDecimal bestPct = BigDecimal.ZERO;
        Long bestCampaignId = null;
        boolean bestHasPd = false;
        boolean bestHasProduct = false;

        if (activeCampaigns != null) {
            for (DiscountCampaign c : activeCampaigns) {
                if (c == null) continue;
                boolean linkPd = hasProductDetailInCampaignSafe(c, pd);
                boolean linkPrd = hasProductInCampaignSafe(c, product);
                if (!linkPd && !linkPrd) continue;

                PctPick pick = pickPctWithLinks(c, pd, linkPd, linkPrd);
                int cmp = pick.pct.compareTo(bestPct);

                if (bestCampaignId == null || cmp > 0) {
                    bestPct = pick.pct;
                    bestCampaignId = c.getId();
                    bestHasPd = linkPd;
                    bestHasProduct = linkPrd;
                } else if (cmp == 0) {
                    // tie-break: ưu tiên campaign có link PD
                    if (linkPd && !bestHasPd) {
                        bestPct = pick.pct;
                        bestCampaignId = c.getId();
                        bestHasPd = true;
                        bestHasProduct = linkPrd;
                    }
                }
            }
        }

        BigDecimal discounted = (bestPct.compareTo(BigDecimal.ZERO) > 0)
                ? sellPrice.multiply(ONE_HUNDRED.subtract(bestPct)).divide(ONE_HUNDRED, 0, RoundingMode.HALF_UP)
                : sellPrice;

        Pricing p = new Pricing();
        p.setCampaignId(bestCampaignId);
        p.setSellPrice(sellPrice);
        p.setPercent(bestPct);
        p.setPercentInt(bestPct.setScale(0, RoundingMode.HALF_UP).intValue());
        p.setDiscountedPrice(vnd(discounted));
        p.setHasPdInCampaign(bestHasPd);
        p.setHasProductInCampaign(bestHasProduct);
        return p;
    }

    private boolean hasProductDetailInCampaignSafe(DiscountCampaign c, ProductDetail pd) {
        if (c == null || pd == null || pd.getId() == null) return false;
        if (c.getProductDetails() == null || c.getProductDetails().isEmpty()) return false;
        for (DiscountCampaignProductDetail link : c.getProductDetails()) {
            if (link == null || link.getProductDetail() == null) continue;
            if (Objects.equals(link.getProductDetail().getId(), pd.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasProductInCampaignSafe(DiscountCampaign c, Product product) {
        if (c == null || product == null || product.getId() == null) return false;
        if (c.getProducts() == null || c.getProducts().isEmpty()) return false;
        // c.getProducts(): giả định là List<DiscountCampaignProduct> với getProduct()
        try {
            return c.getProducts().stream()
                    .filter(Objects::nonNull)
                    .anyMatch(link -> link.getProduct() != null
                            && Objects.equals(link.getProduct().getId(), product.getId()));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lấy % giảm khi đã xác nhận có link:
     * - Có link PD:
     *     + Nếu % riêng != null -> dùng.
     *     + Nếu % riêng = null -> FALLBACK % GLOBAL của campaign.
     * - Không link PD nhưng link Product -> dùng % GLOBAL.
     * - Không có link -> 0%.
     * Tránh tuyệt đối Optional.of(null) gây NPE.
     */
    private PctPick pickPctWithLinks(DiscountCampaign campaign,
                                     ProductDetail pd,
                                     boolean linkPd,
                                     boolean linkPrd) {
        BigDecimal pct = BigDecimal.ZERO;

        if (linkPd) {
            BigDecimal pdPct = null;
            if (campaign.getProductDetails() != null && pd != null && pd.getId() != null) {
                // Tìm đúng link theo PD, rồi map ra discountPercentage (có thể null) — KHÔNG dùng Optional.of(null)
                DiscountCampaignProductDetail found = campaign.getProductDetails().stream()
                        .filter(Objects::nonNull)
                        .filter(link -> link.getProductDetail() != null
                                && Objects.equals(link.getProductDetail().getId(), pd.getId()))
                        .findFirst()
                        .orElse(null);
                if (found != null) {
                    pdPct = found.getDiscountPercentage(); // có thể null
                }
            }
            pct = (pdPct != null) ? pdPct : nz(campaign.getDiscountPercentage());
        } else if (linkPrd) {
            pct = nz(campaign.getDiscountPercentage());
        }

        PctPick pick = new PctPick();
        pick.pct = (pct == null ? BigDecimal.ZERO : pct);
        pick.hasPdInCampaign = linkPd;
        pick.hasProductInCampaign = linkPrd;
        return pick;
    }

    private VerifyPricesResponse.DiffItem buildDiff(
            Long pdId,
            Integer qty,
            BigDecimal feSell,
            BigDecimal feDisc,
            Long feCpnId,
            BigDecimal svSell,
            BigDecimal svDisc,
            Integer svPctInt,
            Long svCpnId,
            String reason
    ) {
        VerifyPricesResponse.DiffItem d = new VerifyPricesResponse.DiffItem();
        d.setProductDetailId(pdId);
        d.setQuantity(qty);
        d.setFeSellPrice(vnd(nz(feSell)));
        d.setFeDiscountedPrice(vnd(nz(feDisc)));
        d.setFePercent(calcPercentInt(vnd(nz(feSell)), vnd(nz(feDisc))));
        d.setFeCampaignId(feCpnId);

        d.setSvSellPrice(vnd(nz(svSell)));
        d.setSvDiscountedPrice(vnd(nz(svDisc)));
        d.setSvPercent(svPctInt == null ? calcPercentInt(vnd(nz(svSell)), vnd(nz(svDisc))) : svPctInt);
        d.setSvCampaignId(svCpnId);

        d.setReason(reason);
        return d;
    }

    // ======================== Internal DTOs ========================
    @Getter
    @Setter
    private static class Pricing {
        private Long campaignId;
        private BigDecimal sellPrice;         // VND, scale 0
        private BigDecimal percent;           // %
        private Integer percentInt;           // % (int)
        private BigDecimal discountedPrice;   // VND, scale 0
        private boolean hasPdInCampaign;      // campaign có link SPCT?
        private boolean hasProductInCampaign; // campaign có link Product?
    }

    private static class PctPick {
        BigDecimal pct;
        boolean hasPdInCampaign;
        boolean hasProductInCampaign;
    }
}
