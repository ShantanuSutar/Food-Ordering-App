package com.shantanu.response;

import com.shantanu.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class PaymentHistoryResponse {
    private Long orderId;
    private Long restaurantId;
    private String restaurantName;
    private Date createdAt;
    private Date paidAt;
    private Long amount;
    private String currency;
    private PaymentStatus paymentStatus;
    private String paymentIntentId;
    private String paymentMethod;
}
