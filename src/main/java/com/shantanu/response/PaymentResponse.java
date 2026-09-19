package com.shantanu.response;

import com.shantanu.model.Address;
import lombok.Data;

@Data
public class PaymentResponse {
    private String payment_url;
    private Address deliveryAddress;

}
