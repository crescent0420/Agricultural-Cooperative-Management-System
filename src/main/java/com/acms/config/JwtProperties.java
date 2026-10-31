package com.acms.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "acms.security.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long ttlMinutes
) {
}