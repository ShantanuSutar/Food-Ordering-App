package com.shantanu.service;

import com.shantanu.model.Food;
import com.shantanu.model.Restaurant;
import com.shantanu.model.User;
import com.shantanu.request.CreateFoodRequest;
import com.shantanu.response.FoodSearchResponse;
import com.shantanu.response.TopMealResponse;

import java.util.List;

public interface FoodService {

    public Food createFood(CreateFoodRequest req, Restaurant restaurant) throws Exception;

    Food updateFood(Long foodId, CreateFoodRequest req, User actor) throws Exception;

    public void deleteFood(Long foodId, User actor) throws Exception;

    public List<Food> getRestaurantsFood(Long RestaurantId, boolean isVegeterian, boolean isNonvegeterian, boolean isSeasonal, String foodCategory);

    public List<FoodSearchResponse> searchFood(String keyword);

    public Food findFoodById(Long foodId) throws Exception;

    public Food updateAvailabilityStatus(Long foodId, User actor) throws Exception;

    public List<TopMealResponse> getTopMeals(int limit);
}
