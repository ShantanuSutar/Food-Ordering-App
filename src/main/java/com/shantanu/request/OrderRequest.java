package com.shantanu.request;

import lombok.Data;

@Data
public class OrderRequest {
    private Long restaurantId;
    private AddressRequest deliveryAddress;
}
