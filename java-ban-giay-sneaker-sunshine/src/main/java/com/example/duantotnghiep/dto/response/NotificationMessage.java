package com.example.duantotnghiep.dto.response;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @NoArgsConstructor @AllArgsConstructor
public class NotificationMessage {
    private String type;        // e.g. ORDER_STATUS
    private String title;       // e.g. "Đơn #HD001 cập nhật"
    private String content;     // e.g. "Trạng thái mới: DANG_GIAO_HANG"
    private String invoiceCode;
    private String nextStatus;  // enum key
    private Instant at;
}
