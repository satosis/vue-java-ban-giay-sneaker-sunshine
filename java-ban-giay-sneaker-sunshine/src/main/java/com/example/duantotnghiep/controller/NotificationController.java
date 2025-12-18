package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.dto.response.NotificationMessage;
import com.example.duantotnghiep.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Client publish: /app/notify.read  (ví dụ đánh dấu đã đọc)
    @MessageMapping("/notify.read")
    public void onClientMarkedRead(Authentication auth) {
        // auth.getName() là username từ JWT trong WS
        // TODO: cập nhật DB nếu cần
    }

    // Test nhanh: GET /api/notify/test?u=<username>
    @GetMapping("/api/notify/test")
    public String test(@RequestParam String u) {
        var msg = new NotificationMessage(
                "TEST", "Ping", "Test từ server", null, null, Instant.now()
        );
        notificationService.sendToUser(u, msg);
        return "sent to " + u;
    }
}
