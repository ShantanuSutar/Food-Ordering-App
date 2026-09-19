package com.shantanu.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopMealResponse {
    private Long id;
    private String name;
    private String image;
    private Long price;
    private Long restaurantId;
    private String restaurantName;
    private String restaurantCity;
    private String restaurantCuisineType;
}
