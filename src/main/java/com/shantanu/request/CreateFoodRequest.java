package com.shantanu.request;

import lombok.Data;

import java.util.List;

@Data
public class CreateFoodRequest {
    private String name;
    private String description;
    private Long price;

    private Long categoryId;
    private List<String> images;

    private Long restaurantId;
    private Boolean vegetarian;
    private Boolean seasonal;
    private List<Long> ingredientIds;
}
