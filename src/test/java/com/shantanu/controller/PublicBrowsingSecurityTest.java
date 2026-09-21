package com.shantanu.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.frontend-url=https://dinehub-ui.vercel.app")
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

    @Test
    void deployedFrontendCanPreflightSignup() throws Exception {
        mockMvc.perform(options("/auth/signup")
                        .header(HttpHeaders.ORIGIN, "https://dinehub-ui.vercel.app")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://dinehub-ui.vercel.app"
                ));
    }
}
