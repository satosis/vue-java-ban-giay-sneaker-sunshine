package com.example.duantotnghiep.controller;

import com.example.duantotnghiep.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mail")
public class MailTestController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/test")
    public ResponseEntity<String> testMail() {
        try {
            emailService.sendEmail(
                    "nguoinhan@example.com", // Thay bằng email thật để test
                    "✅ Test gửi mail thành công",
                    "<h3>Spring Boot đã gửi được email 🎉</h3><p>Chúc mừng bạn!</p>"
            );
            return ResponseEntity.ok("✅ Đã gửi mail thành công!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Gửi mail thất bại: " + e.getMessage());
        }
    }
}
