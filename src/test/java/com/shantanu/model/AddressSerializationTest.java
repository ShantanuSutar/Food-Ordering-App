package com.shantanu.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressSerializationTest {

    @Test
    void userProfileSerializationUsesOnlyCanonicalAddressFieldNames() throws Exception {
        Address address = new Address();
        address.setId(7L);
        address.setFullName("Asha Patil");
        address.setStreetAddress("12 Market Road");
        address.setCity("Pune");
        address.setState("Maharashtra");
        address.setPostalCode("411001");
        address.setCountry("India");

        User user = new User();
        user.setAddresses(new ArrayList<>());
        user.getAddresses().add(address);

        String json = new ObjectMapper().writeValueAsString(user);

        assertTrue(json.contains("\"addresses\""));
        assertTrue(json.contains("\"streetAddress\""));
        assertTrue(json.contains("\"state\":\"Maharashtra\""));
        assertTrue(json.contains("\"postalCode\""));
        assertFalse(json.contains("stateProvince"));
        assertFalse(json.contains("pincode"));
    }
}
