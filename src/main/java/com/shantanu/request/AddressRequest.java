package com.shantanu.request;

import lombok.Data;

@Data
public class AddressRequest {
    private Long id;
    private String fullName;
    private String streetAddress;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
