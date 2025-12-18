package com.example.duantotnghiep.service;

import com.example.duantotnghiep.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messaging;
    private final UserRepository userRepository;

    private static final String TOPIC_BROADCAST = "/topic/broadcast"; // KHÔNG dùng nữa
    private static final String USER_QUEUE_NOTI = "/queue/notifications";

    private static String topicCustomer(long customerId) {
        return "/topic/customer." + customerId;
    }

    public void sendBroadcast(Object payload) {
        // KHÔNG dùng để tránh lộ thông tin
        // messaging.convertAndSend(TOPIC_BROADCAST, payload);
    }

    /** Giữ nguyên: bắn topic theo KH (đã có guard) */
    public void sendToCustomerId(Long customerId, Object payload) {
        if (customerId != null && payload != null) {
            messaging.convertAndSend(topicCustomer(customerId), payload);
        }
    }

    /** Gửi tới tất cả account thuộc KH qua /user/queue/notifications (an toàn nhất) */
    public void sendToAllUsersOfCustomer(Long customerId, Object payload) {
        if (customerId == null || payload == null) return;
        userRepository.findAllByCustomerId(customerId).forEach(u ->
                messaging.convertAndSendToUser(u.getUsername(), USER_QUEUE_NOTI, payload)
        );
    }

    public void sendToUser(String username, Object payload) {
        if (username != null && !username.isBlank() && payload != null) {
            messaging.convertAndSendToUser(username, USER_QUEUE_NOTI, payload);
        }
    }

    public void sendToEmployeeId(Long employeeId, Object payload) {
        if (employeeId != null && payload != null) {
            messaging.convertAndSend("/topic/employee." + employeeId, payload);
        }
    }
}



