package com.Tta.QLCSVC.DHNT.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {
    private String secret = "qlcsvc-ai-secret-key-dhnt-2026-very-long-secret-key-for-jwt-token-generation";
    private long expiration = 86400000; // 24 hours
    private long refreshExpiration = 604800000; // 7 days
}
