package com.shantanu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtConfig {

    @Value("${JWT_SECRET}")
    private String secretKey;

    public String getSecretKey() {
        return secretKey;
    }
}