package com.shantanu.service;

import com.shantanu.model.Order;
import com.shantanu.response.PaymentResponse;
import com.shantanu.response.PaymentVerificationResponse;
import com.stripe.exception.StripeException;

public interface PaymentService {
    public PaymentResponse createPaymentLink(Order order) throws StripeException;
    PaymentVerificationResponse verifyAndFinalizePayment(String sessionId, Long orderId, Long userId) throws Exception;
    void processWebhook(String payload, String signatureHeader) throws Exception;
}
