package com.njxs.lims.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("lims.auth")
public record AuthProperties(
    String jwtSecret,
    long accessTokenTtlMinutes,
    long refreshTokenTtlDays
) {
}
