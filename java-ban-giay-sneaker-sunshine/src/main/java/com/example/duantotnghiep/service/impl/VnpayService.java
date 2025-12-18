package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.config.HMACUtilVnPay;
import com.example.duantotnghiep.dto.response.VnpayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VnpayService {

    private static final String VNP_TMNCODE = "MQ47A12E"; // Mã TMN test
    private static final String VNP_HASH_SECRET = "TD1JJ3D5ZGD8TROGP300QTTW4RO9M3C7"; // Key test
    private static final String VNP_BASE_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static final String RETURN_URL = "http://localhost:5174/payment-result";

    public VnpayResponse createVnpayOrder(String orderId, BigDecimal amount, String description) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("vnp_Version", "2.1.0");
            params.put("vnp_Command", "pay");
            params.put("vnp_TmnCode", VNP_TMNCODE);
            params.put("vnp_Amount", String.valueOf(amount.multiply(BigDecimal.valueOf(100)).longValue()));
            params.put("vnp_CurrCode", "VND");
            params.put("vnp_TxnRef", orderId);
            params.put("vnp_OrderInfo", description);
            params.put("vnp_OrderType", "other");
            params.put("vnp_Locale", "vn");
            params.put("vnp_ReturnUrl", RETURN_URL);
            params.put("vnp_IpAddr", "127.0.0.1");
            params.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

            List<String> sortedKeys = new ArrayList<>(params.keySet());
            Collections.sort(sortedKeys);

            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            for (int i = 0; i < sortedKeys.size(); i++) {
                String key = sortedKeys.get(i);
                String value = params.get(key);

                // ✅ Không encode khi tạo hashData
                hashData.append(key).append('=').append(value);

                // ✅ Encode khi tạo query string
                query.append(URLEncoder.encode(key, StandardCharsets.US_ASCII)).append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));

                if (i < sortedKeys.size() - 1) {
                    hashData.append('&');
                    query.append('&');
                }
            }

            // 👉 Log raw hashData để so khớp với hướng dẫn VNPay
            log.info("🔐 hashData: {}", hashData);

            String secureHash = HMACUtilVnPay.hmacSHA512(VNP_HASH_SECRET, hashData.toString());

            // 👉 Log secureHash đã tính
            log.info("🔑 secureHash: {}", secureHash);

            query.append("&vnp_SecureHash=").append(secureHash);

            String paymentUrl = VNP_BASE_URL + "?" + query;

            // 👉 Log URL đầy đủ gửi tới VNPay
            log.info("✅ VNPay URL: {}", paymentUrl);

            return new VnpayResponse(orderId, paymentUrl, "Tạo đơn VNPay thành công", 1);

        } catch (Exception e) {
            log.error("❌ Lỗi khi tạo đơn VNPay: ", e);
            throw new RuntimeException("❌ Lỗi tạo thanh toán VNPay: " + e.getMessage());
        }
    }


}



