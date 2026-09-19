package com.shantanu.request;

import lombok.Data;

@Data
public class PaymentVerificationRequest {
    private String sessionId;
    private Long orderId;
}
