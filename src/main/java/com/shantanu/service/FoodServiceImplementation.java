package com.shantanu.service;

import com.shantanu.model.Category;
import com.shantanu.model.Food;
import com.shantanu.model.Restaurant;
import com.shantanu.repository.FoodRepository;
import com.shantanu.request.CreateFoodRequest;
import com.shantanu.response.FoodSearchResponse;
import com.shantanu.response.TopMealResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FoodServiceImplementation implements FoodService {

    @Autowired
    private FoodRepository foodRepository;

    @Override
    public Food createFood(CreateFoodRequest req, Category category, Restaurant restaurant) {
        Food food = new Food();
        food.setFoodCategory(category);
        food.setRestaurant(restaurant);
        food.setDescription(req.getDescription());
        food.setImages(req.getImages());
        food.setName(req.getName());
        food.setPrice(req.getPrice());
        food.setIngredients(req.getIngredients());
        food.setSeasonal(req.isSeasonal());
        food.setVegetarian(req.isVegetarian());
        food.setCreationDate(new Date());

        Food savedFood = foodRepository.save(food);
        restaurant.getFoods().add(savedFood);

        return savedFood;
    }

    @Override
    public void deleteFood(Long foodId) throws Exception {
        Food food = findFoodById(foodId);
        food.setRestaurant(null);
        foodRepository.save(food);
    }

    @Override
    public List<Food> getRestaurantsFood(Long restaurantId, boolean isVegeterian, boolean isNonvegeterian, boolean isSeasonal, String foodCategory) {
        List<Food> foods = foodRepository.findByRestaurantId(restaurantId);

        if(isVegeterian){
            foods = filterByVegetarian(foods, isVegeterian);
        }

        if(isNonvegeterian){
            foods = filterByNonVegetarian(foods, isNonvegeterian);
        }

        if(isSeasonal){
            foods = filterByIsSeasonal(foods, isSeasonal);
        }

        if (foodCategory != null
                && !foodCategory.isBlank()
                && !foodCategory.equalsIgnoreCase("null")) {

            foods = filterByCategory(foods, foodCategory);
        }
        return foods;
    }

    private List<Food> filterByCategory(List<Food> foods, String foodCategory) {
        return foods.stream().filter(food -> {
            if(food.getFoodCategory() != null){
                return food.getFoodCategory().getName().equals(foodCategory);
            }

            return false;
        }).collect(Collectors.toList());
    }

    private List<Food> filterByIsSeasonal(List<Food> foods, boolean isSeasonal) {
        return foods.stream().filter(food -> food.isSeasonal() == isSeasonal).collect(Collectors.toList());
    }

    private List<Food> filterByNonVegetarian(List<Food> foods, boolean isNonvegeterian) {
        return foods.stream().filter(food -> food.isVegetarian() == false).collect(Collectors.toList());
    }

    private List<Food> filterByVegetarian(List<Food> foods, boolean isVegeterian) {
        return foods.stream().filter(food -> food.isVegetarian() == isVegeterian).collect(Collectors.toList());
    }

    @Override
    public List<FoodSearchResponse> searchFood(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        return foodRepository.searchFood(keyword.trim(), PageRequest.of(0, 12))
                .stream()
                .map(food -> {
                    Category category = food.getFoodCategory();
                    Restaurant restaurant = food.getRestaurant();
                    String image = food.getImages() == null
                            ? null
                            : food.getImages().stream()
                                    .filter(value -> value != null && !value.isBlank())
                                    .findFirst()
                                    .orElse(null);
                    String city = restaurant.getAddress() == null
                            ? null
                            : restaurant.getAddress().getCity();

                    return new FoodSearchResponse(
                            food.getId(),
                            food.getName(),
                            food.getDescription(),
                            food.getPrice(),
                            image,
                            category == null ? null : category.getId(),
                            category == null ? null : category.getName(),
                            restaurant.getId(),
                            restaurant.getName(),
                            city
                    );
                })
                .toList();
    }

    @Override
    public Food findFoodById(Long foodId) throws Exception {
        Optional<Food> optionalFood = foodRepository.findById(foodId);

        if(optionalFood.isEmpty()){
            throw new Exception("Food does not exist!");
        }
        return optionalFood.get();
    }

    @Override
    public Food updateAvailabilityStatus(Long foodId) throws Exception {
        Food food = findFoodById(foodId);
        food.setAvailable(!food.isAvailable());
        return foodRepository.save(food);
    }

    @Override
    public List<TopMealResponse> getTopMeals(int limit) {
        int resultLimit = Math.max(1, Math.min(limit, 12));

        return foodRepository.findTopAvailableFoods(PageRequest.of(0, resultLimit))
                .stream()
                .map(food -> {
                    Restaurant restaurant = food.getRestaurant();
                    String image = food.getImages() == null
                            ? null
                            : food.getImages().stream()
                                    .filter(value -> value != null && !value.isBlank())
                                    .findFirst()
                                    .orElse(null);
                    String city = restaurant.getAddress() == null
                            ? null
                            : restaurant.getAddress().getCity();

                    return new TopMealResponse(
                            food.getId(),
                            food.getName(),
                            image,
                            food.getPrice(),
                            restaurant.getId(),
                            restaurant.getName(),
                            city,
                            restaurant.getCuisineType()
                    );
                })
                .toList();
    }
}
