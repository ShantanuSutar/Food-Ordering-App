package com.shantanu.service;

import com.shantanu.model.Category;
import com.shantanu.model.Food;
import com.shantanu.model.Restaurant;
import com.shantanu.request.CreateFoodRequest;

import java.util.List;

public interface FoodService {

    public Food createFood(CreateFoodRequest req, Category category, Restaurant restaurant);

    public void deleteFood(Long foodId) throws Exception;

    public List<Food> getRestaurantsFood(Long RestaurantId, boolean isVegeterian, boolean isNonvegeterian, boolean isSeasonal, String foodCategory);

    public List<Food> searchFood(String keyword);

    public Food findFoodById(Long foodId) throws Exception;

    public Food updateAvailabilityStatus(Long foodId) throws Exception;
}
