package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.dto.response.FavouriteResponse;
import com.example.duantotnghiep.dto.response.RatingResponse;
import com.example.duantotnghiep.dto.response.TopRatedProductDTO;
import com.example.duantotnghiep.model.Customer;
import com.example.duantotnghiep.model.ProductRatingView;
import com.example.duantotnghiep.model.User;
import com.example.duantotnghiep.repository.FavoriteProductRepository;
import com.example.duantotnghiep.repository.InvoiceRepository2;
import com.example.duantotnghiep.repository.UserRepository;
import com.example.duantotnghiep.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RatingServiceIpml implements RatingService {
    private final FavoriteProductRepository favoriteProductRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository2 invoiceRepository;
    @Override
    public Map<String, Object> getRatingOfProduct(Long productId) {
        var viewOpt = favoriteProductRepository.findRatingByProductId(productId);

        if (viewOpt.isEmpty()) {
            return Map.of(
                    "productId", productId,
                    "avgRating", 0.0,
                    "totalReviews", 0,
                    "distribution", Map.of(1,0,2,0,3,0,4,0,5,0)
            );
        }

        var v = viewOpt.get();
        Map<Integer, Long> dist = new java.util.LinkedHashMap<>();
        dist.put(1, v.getStar1());
        dist.put(2, v.getStar2());
        dist.put(3, v.getStar3());
        dist.put(4, v.getStar4());
        dist.put(5, v.getStar5());

        return Map.of(
                "productId", v.getProductId(),
                "avgRating", round1(v.getAvgRating()),
                "totalReviews", v.getTotalReviews(),
                "distribution", dist
        );
    }

    @Override
    public List<Map<String, Object>> getRatingsOfProducts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return List.of();
        var list = favoriteProductRepository.findRatingsByProductIds(productIds);
        Map<Long, ProductRatingView> byId = new java.util.HashMap<>();
        for (var v : list) byId.put(v.getProductId(), v);

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Long pid : productIds) {
            var v = byId.get(pid);
            if (v == null) {
                result.add(Map.of(
                        "productId", pid,
                        "avgRating", 0.0,
                        "totalReviews", 0,
                        "distribution", Map.of(1,0,2,0,3,0,4,0,5,0)
                ));
            } else {
                result.add(Map.of(
                        "productId", v.getProductId(),
                        "avgRating", round1(v.getAvgRating()),
                        "totalReviews", v.getTotalReviews(),
                        "distribution", Map.of(
                                1, v.getStar1(),
                                2, v.getStar2(),
                                3, v.getStar3(),
                                4, v.getStar4(),
                                5, v.getStar5()
                        )
                ));
            }
        }
        return result;
    }

    @Override
    public List<FavouriteResponse> getDeliveredInvoicesForReview(Integer onlyUnrated, String keyword, Date dateFrom, Date dateTo) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với username: " + username));

        Customer c = user.getCustomer();
        if (c == null) {
            throw new RuntimeException("Người dùng không phải là khách hàng.");
        }
        List<Object[]> rows = invoiceRepository.findDeliveredRows(c.getId(), nullSafe(onlyUnrated), keyword, dateFrom, dateTo);

        Map<Long, FavouriteResponse> grouped = new LinkedHashMap<>();

        for (Object[] r : rows) {
            int i = 0;
            Long invoiceId            = toLong(r[i++]);
            String invoiceCode        = toString(r[i++]);
            Date deliveredAt          = toDate(r[i++]);
            BigDecimal finalAmount    = toBigDecimal(r[i++]);
            String customerName       = toString(r[i++]);

            Long detailId             = toLong(r[i++]);
            Long productId             = toLong(r[i++]);
            String productName        = toString(r[i++]);
            String productCode        = toString(r[i++]);
            String sizeName           = toString(r[i++]);
            String colorName          = toString(r[i++]);
            BigDecimal sellPrice      = toBigDecimal(r[i++]);
            Integer discountPercent   = toInteger(r[i++]);
            BigDecimal discountAmount = toBigDecimal(r[i++]);
            BigDecimal discountedPrice= toBigDecimal(r[i++]);
            Integer quantity          = toInteger(r[i++]);
            String invoiceCodeDetail  = toString(r[i++]);
            BigDecimal totalPrice     = toBigDecimal(r[i++]);
            BigDecimal finalTotalPrice= toBigDecimal(r[i++]);
            Integer rate              = toInteger(r[i++]);
            String comment            = toString(r[i++]);
            boolean rated             = toBoolean(r[i++]);
            byte[] image              = (byte[]) r[i++]; // có thể null

            // ------- Group theo invoice -------
            FavouriteResponse inv = grouped.computeIfAbsent(invoiceId, id -> {
                FavouriteResponse f = new FavouriteResponse();
                f.setInvoiceId(invoiceId);
                f.setInvoiceCode(invoiceCode);
                f.setDeliveredAt(deliveredAt);
                f.setFinalAmount(finalAmount);
                f.setCustomerName(customerName);
                f.setInvoiceDetail(new ArrayList<>());
                return f;
            });

            RatingResponse d = new RatingResponse();
            d.setId(detailId);
            d.setProductId(productId);
            d.setProductName(productName);
            d.setProductCode(productCode);
            d.setSizeName(sizeName);
            d.setColorName(colorName);
            d.setSellPrice(sellPrice);
            d.setDiscountPercentage(discountPercent);
            d.setDiscountAmount(discountAmount);
            d.setDiscountedPrice(discountedPrice);
            d.setQuantity(quantity);
            d.setInvoiceCodeDetail(invoiceCodeDetail);
            d.setTotalPrice(totalPrice);
            d.setFinalTotalPrice(finalTotalPrice);
            d.setCustomerName(customerName);
            d.setRate(rate);
            d.setComment(comment);
            d.setRated(rated);
            d.setImage(image);

            inv.getInvoiceDetail().add(d);
        }

        return new ArrayList<>(grouped.values());
    }

    private static Integer nullSafe(Integer v) { return v == null ? 0 : v; }

    private static String toString(Object o) { return o == null ? null : o.toString(); }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long l) return l;
        if (o instanceof Number n) return n.longValue();
        return Long.valueOf(o.toString());
    }

    private static Integer toInteger(Object o) {
        if (o == null) return null;
        if (o instanceof Integer i) return i;
        if (o instanceof Number n) return n.intValue();
        return Integer.valueOf(o.toString());
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(o.toString());
    }

    private static Date toDate(Object o) {
        if (o == null) return null;
        if (o instanceof Date d) return d;
        if (o instanceof Timestamp ts) return new Date(ts.getTime());
        return null;
    }

    private static boolean toBoolean(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.intValue() != 0;
        return Boolean.parseBoolean(o.toString());
    }

    private double round1(Double d) {
        if (d == null) return 0.0;
        return Math.round(d * 10.0) / 10.0; // làm tròn 1 chữ số thập phân (vd 4.3)
    }

    @Override
    public List<TopRatedProductDTO> getTop5RatedWithSales(long minReviews) {
        return favoriteProductRepository.findTopRatedWithSalesAndFavCount(
                org.springframework.data.domain.PageRequest.of(0, 5),
                Math.max(0, minReviews)
        );
    }

}
