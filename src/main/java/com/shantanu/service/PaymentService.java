package com.shantanu.service;

import com.shantanu.model.Order;
import com.shantanu.response.PaymentResponse;
import com.shantanu.response.PaymentHistoryResponse;
import com.shantanu.response.PaymentVerificationResponse;
import com.stripe.exception.StripeException;

import java.util.List;

public interface PaymentService {
    public PaymentResponse createPaymentLink(Order order) throws StripeException;
    PaymentVerificationResponse verifyAndFinalizePayment(String sessionId, Long orderId, Long userId) throws Exception;
    void processWebhook(String payload, String signatureHeader) throws Exception;
    void cancelPendingPayment(Order order) throws Exception;
    List<PaymentHistoryResponse> getPaymentHistory(Long userId);
}
