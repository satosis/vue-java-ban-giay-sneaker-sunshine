// com.example.duantotnghiep.config.WsJwtChannelInterceptor.java
package com.example.duantotnghiep.config;

import com.example.duantotnghiep.JWT.JwtUtil;
import com.example.duantotnghiep.model.Customer;
import com.example.duantotnghiep.model.User;
import com.example.duantotnghiep.repository.UserRepository;
import com.example.duantotnghiep.service.impl.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class WsJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository; // thêm: để fallback customerId khi JWT không có

    private static final Pattern CUSTOMER_TOPIC = Pattern.compile("^/topic/customer\\.(\\d+)$");
    private static final String ATTR_CUSTOMER_ID = "customerId";

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        var accessor = MessageHeaderAccessor.getAccessor(message, org.springframework.messaging.simp.stomp.StompHeaderAccessor.class);
        if (accessor == null) return message;

        // ==== 1) CONNECT: xác thực + gắn customerId vào session ====
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            @SuppressWarnings("unchecked")
            Map<String, List<String>> nativeHeaders = (Map<String, List<String>>) accessor.getHeader("nativeHeaders");
            String bearer = nativeHeaders != null && nativeHeaders.get("Authorization") != null
                    ? nativeHeaders.get("Authorization").get(0) : null;

            String token = (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;

            if (token != null && !token.isBlank()) {
                String username = jwtUtil.extractUsername(token);
                if (username != null) {
                    UserDetails ud = userDetailsService.loadUserByUsername(username);
                    if (jwtUtil.validateToken(token, ud)) {
                        // Lấy customerId từ JWT claim trước
                        Long customerId = null;
                        try {
                            Object claim = jwtUtil.getClaim(token, "customerId");
                            if (claim != null) {
                                customerId = Long.valueOf(String.valueOf(claim));
                            }
                        } catch (Exception ignore) {}

                        // Fallback DB nếu cần
                        if (customerId == null) {
                            customerId = userRepository.findByUsername(username)
                                    .map(User::getCustomer)
                                    .map(Customer::getId)
                                    .orElse(null);
                        }

                        // Gắn Authentication + nhét customerId vào session
                        var auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                        accessor.setUser(auth);
                        accessor.getSessionAttributes().put(ATTR_CUSTOMER_ID, customerId);
                    } else {
                        throw new IllegalArgumentException("JWT không hợp lệ cho WebSocket CONNECT");
                    }
                }
            }
        }

        // ==== 2) SUBSCRIBE: chặn đăng ký sai customer ====
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String dest = accessor.getDestination();
            if (dest != null) {
                var m = CUSTOMER_TOPIC.matcher(dest);
                if (m.matches()) {
                    Long requestedCustomerId = Long.valueOf(m.group(1));
                    Object cid = accessor.getSessionAttributes().get(ATTR_CUSTOMER_ID);
                    Long principalCustomerId = (cid == null) ? null : Long.valueOf(String.valueOf(cid));

                    if (principalCustomerId == null || !principalCustomerId.equals(requestedCustomerId)) {
                        throw new AccessDeniedException("Không được phép subscribe topic của khách hàng khác.");
                    }
                }
            }
        }

        return message;
    }
}

