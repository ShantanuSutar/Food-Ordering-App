package com.shantanu.request;

import com.shantanu.model.Category;
import com.shantanu.model.IngredientsItem;
import lombok.Data;

import java.util.List;

@Data
public class CreateFoodRequest {
    private String name;
    private String description;
    private Long price;

    private Category category;
    private List<String> images;

    private long restaurantId;
    private boolean vegetarian;
    private boolean seasonal;
    private List<IngredientsItem> ingredients;
}
