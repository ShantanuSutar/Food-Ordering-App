package com.shantanu.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestaurantSerializationTest {

    @Test
    void publicRestaurantJsonDoesNotExposeOwnerAccount() throws Exception {
        User owner = new User();
        owner.setId(8L);
        owner.setEmail("owner@example.com");
        owner.setPassword("secret-hash");

        Restaurant restaurant = new Restaurant();
        restaurant.setId(3L);
        restaurant.setName("DineHub Kitchen");
        restaurant.setOwner(owner);

        String json = new ObjectMapper().writeValueAsString(restaurant);

        assertTrue(json.contains("\"name\":\"DineHub Kitchen\""));
        assertFalse(json.contains("\"owner\""));
        assertFalse(json.contains("owner@example.com"));
        assertFalse(json.contains("secret-hash"));
    }
}
