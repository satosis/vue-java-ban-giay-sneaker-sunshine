package com.example.duantotnghiep.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VnpayResponse {
    private String orderId;          // Mã đơn hàng (vnp_TxnRef)
    private String paymentUrl;       // URL redirect đến VNPay
    private String returnMessage;    // Thông báo từ VNPay
    private int returnCode;          // Mã trả về (00 là thành công)
}

