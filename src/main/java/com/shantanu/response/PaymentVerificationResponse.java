package com.shantanu.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentVerificationResponse {
    private boolean verified;
    private String message;
    private String paymentStatus;
}
