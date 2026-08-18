package com.shantanu.service;

import com.shantanu.model.Order;
import com.shantanu.response.PaymentResponse;
import com.stripe.exception.StripeException;

public interface PaymentService {
    public PaymentResponse createPaymentLink(Order order) throws StripeException;
}
