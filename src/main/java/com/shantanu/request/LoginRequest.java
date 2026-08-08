package com.shantanu.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
