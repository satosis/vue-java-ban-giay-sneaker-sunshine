package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.config.HMACUtil;
import com.example.duantotnghiep.dto.response.ZaloPayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZaloPayService {

    private static final String APP_ID = "2554";
    private static final String KEY1 = "sdngKKJmqEMzvh5QQcdD2A9XBSKUNaYn";
    private static final String ENDPOINT = "https://sandbox.zalopay.com.vn/v001/tpe/createorder";

    public ZaloPayResponse createZaloPayOrder(
            String customerPhone,
            BigDecimal amount,
            String description,
            String appTransId // ✅ được truyền từ ngoài vào (dựa theo invoiceId)
    ) throws Exception {

        long appTime = System.currentTimeMillis();

        // ✅ Embed redirect + callback URL
        Map<String, Object> embeddata = new HashMap<>();
        embeddata.put("redirecturl", "http://localhost:5174/payment-result?app_trans_id=" + appTransId);
        embeddata.put("callbackurl", "https://61a4-2402-800-619d-8f7d-e15c-c45c-5cf8-d1db.ngrok-free.app/api/payment/zalo/callback");

        // ✅ Sản phẩm thanh toán (cần ít nhất 1 item)
        Map<String, Object> item = new HashMap<>();
        item.put("itemid", "001");
        item.put("itemname", "Thanh toán đơn hàng");
        item.put("itemprice", amount.longValue());
        item.put("itemquantity", 1);
        List<Map<String, Object>> items = Collections.singletonList(item);

        // ✅ Chuẩn bị dữ liệu đơn hàng
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("appid", APP_ID);
        order.put("apptransid", appTransId);
        order.put("apptime", appTime);
        order.put("appuser", customerPhone);
        order.put("amount", amount.longValue());
        order.put("description", description);
        order.put("item", new JSONObject(items).toString());
        order.put("embeddata", new JSONObject(embeddata).toString());

        // ✅ Ký MAC
        String dataToSign = String.join("|",
                APP_ID,
                appTransId,
                customerPhone,
                String.valueOf(amount.longValue()),
                String.valueOf(appTime),
                new JSONObject(embeddata).toString(),
                new JSONObject(items).toString()
        );
        String mac = HMACUtil.HMacHexStringEncode(HMACUtil.HMACSHA256, KEY1, dataToSign);
        order.put("mac", mac);

        log.info("📤 Dữ liệu gửi ZaloPay: {}", order);
        log.info("🔐 MAC: {}", mac);

        // ✅ Gửi request
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(ENDPOINT);

            List<NameValuePair> params = new ArrayList<>();
            for (Map.Entry<String, Object> entry : order.entrySet()) {
                params.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
            }
            post.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }

                JSONObject jsonResponse = new JSONObject(result.toString());
                log.info(" Phản hồi ZaloPay: {}", jsonResponse.toString(2));

                int returnCode = jsonResponse.optInt("returncode");
                String returnMessage = jsonResponse.optString("returnmessage");
                String orderUrl = jsonResponse.optString("orderurl");
                String zpTransToken = jsonResponse.optString("zptranstoken");

                if (returnCode != 1 || orderUrl == null || zpTransToken == null || orderUrl.isEmpty()) {
                    throw new RuntimeException("❌ Lỗi tạo đơn hàng ZaloPay: " + returnMessage + " (Code: " + returnCode + ")");
                }

                return new ZaloPayResponse(appTransId, orderUrl, zpTransToken, returnCode, returnMessage);
            }

        } catch (IOException e) {
            log.error("❌ Lỗi khi gửi request ZaloPay: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể kết nối đến ZaloPay", e);
        }
    }

    public JSONObject queryOrder(String appTransId) throws Exception {
        String data = APP_ID + "|" + appTransId + "|" + KEY1;
        String mac = HMACUtil.HMacHexStringEncode(HMACUtil.HMACSHA256, KEY1, data);

        Map<String, String> params = new HashMap<>();
        params.put("appid", APP_ID);
        params.put("apptransid", appTransId);
        params.put("mac", mac);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost("https://sandbox.zalopay.com.vn/v001/tpe/getstatusbyapptransid");

            List<NameValuePair> body = new ArrayList<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                body.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            post.setEntity(new UrlEncodedFormEntity(body, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                String result = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))
                        .lines().collect(Collectors.joining("\n"));
                return new JSONObject(result);
            }
        }
    }



}



