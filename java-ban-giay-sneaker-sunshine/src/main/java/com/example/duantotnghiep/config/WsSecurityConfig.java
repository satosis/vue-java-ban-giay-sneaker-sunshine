// com.example.duantotnghiep.config.WsSecurityConfig.java
package com.example.duantotnghiep.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@RequiredArgsConstructor
public class WsSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final WsJwtChannelInterceptor wsJwtChannelInterceptor;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(wsJwtChannelInterceptor);
    }
}
