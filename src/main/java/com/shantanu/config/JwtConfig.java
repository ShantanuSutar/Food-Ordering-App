package com.shantanu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtConfig {

    @Value("${JWT_SECRET:default-secret-key-that-is-long-enough-for-hs256-algorithm}")
    private String secretKey;

    public String getSecretKey() {
        return secretKey;
    }
}