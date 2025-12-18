package com.example.duantotnghiep.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountEmailService {

    private final EmailService emailService;

    /**
     * Gửi email tài khoản mới ở chế độ bất đồng bộ để không chặn HTTP request.
     */
    @Async
    public void sendAccountCreatedEmail(String recipientEmail, String customerName,
                                        String username, String rawPassword) {
        try {
            String subject = "🎉 Tài khoản của bạn đã được tạo thành công!";

            String content = "<h3>Xin chào " + customerName + ",</h3>"
                    + "<p>🎉 Chúc mừng! Tài khoản của bạn tại hệ thống Sneaker Sunshine đã được tạo thành công.</p>"
                    + "<p>Dưới đây là thông tin đăng nhập của bạn:</p>"
                    + "<ul>"
                    + "<li><strong>Tên đăng nhập:</strong> " + username + "</li>"
                    + "<li><strong>Mật khẩu:</strong> " + rawPassword + "</li>"
                    + "</ul>"
                    + "<p>Bạn vui lòng đăng nhập và đổi mật khẩu để đảm bảo an toàn.</p>"
                    + "<br><p>Trân trọng,<br>Đội ngũ hỗ trợ</p>";

            emailService.sendEmail(recipientEmail, subject, content);
            log.info("[AccountEmailService] Sent account email to {}", recipientEmail);
        } catch (Exception ex) {
            // Không để exception làm fail request chính
            log.error("[AccountEmailService] Failed to send account email to {}: {}", recipientEmail, ex.getMessage(), ex);
        }
    }

    @Async
    public void sendPasswordResetEmail(String recipientEmail,
                                       String customerName,
                                       String username,
                                       String tempPassword) {
        try {
            String subject = "🔐 Yêu cầu đặt lại mật khẩu – Sneaker Sunshine";

            String content =
                    "<div style='font-family: Inter, Arial, sans-serif; line-height:1.6; color:#111;'>"
                            + "  <h3>Xin chào " + (customerName == null ? "bạn" : customerName) + ",</h3>"
                            + "  <p>Chúng tôi đã nhận được yêu cầu <strong>đặt lại mật khẩu</strong> cho tài khoản của bạn tại <strong>Sneaker Sunshine</strong>.</p>"
                            + "  <p>Thông tin đăng nhập tạm thời:</p>"
                            + "  <ul>"
                            + "    <li><strong>Tên đăng nhập:</strong> " + username + "</li>"
                            + "    <li><strong>Mật khẩu tạm:</strong> <code style='background:#f6f6f6; padding:2px 6px; border-radius:4px;'>"
                            +          tempPassword + "</code></li>"
                            + "  </ul>"
                            + "  <p style='margin:16px 0 8px;'>"
                            + "    <strong>Lưu ý bảo mật:</strong> Vui lòng đăng nhập và <strong>đổi mật khẩu</strong> ngay trong phần Tài khoản &gt; Đổi mật khẩu."
                            + "  </p>"
                            + "  <p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này hoặc liên hệ hỗ trợ để được giúp đỡ.</p>"
                            + "  <br>"
                            + "  <p>Trân trọng,<br>Đội ngũ hỗ trợ Sneaker Sunshine</p>"
                            + "</div>";

            emailService.sendEmail(recipientEmail, subject, content);
            log.info("[AccountEmailService] Sent reset-password email to {}", recipientEmail);
        } catch (Exception ex) {
            log.error("[AccountEmailService] Failed to send reset-password email to {}: {}", recipientEmail, ex.getMessage(), ex);
        }
    }

}
