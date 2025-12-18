package com.example.duantotnghiep.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "zalopay")
@Getter
@Setter
public class ZaloPayConfig {
    private String appid;
    private String key1;
    private String key2;
    private String endpoint;
    private String callback;
}

