package com.shantanu.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicBrowsingSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousUsersCanBrowseRestaurantDiscoveryData() throws Exception {
        mockMvc.perform(get("/api/restaurants"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/food/restaurant/999"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/category/restaurant/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousUsersStillCannotModifyFavourites() throws Exception {
        mockMvc.perform(put("/api/restaurants/1/add-favourites"))
                .andExpect(status().is4xxClientError());
    }
}
