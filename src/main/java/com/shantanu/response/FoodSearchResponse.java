package com.shantanu.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FoodSearchResponse {
    private Long id;
    private String name;
    private String description;
    private Long price;
    private String image;
    private Long categoryId;
    private String categoryName;
    private Long restaurantId;
    private String restaurantName;
    private String restaurantCity;
}
