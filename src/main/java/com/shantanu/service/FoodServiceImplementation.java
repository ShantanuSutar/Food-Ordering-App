package com.shantanu.service;

import com.shantanu.model.Category;
import com.shantanu.model.Food;
import com.shantanu.model.IngredientsItem;
import com.shantanu.model.Restaurant;
import com.shantanu.model.User;
import com.shantanu.repository.CategoryRepository;
import com.shantanu.repository.FoodRepository;
import com.shantanu.repository.IngredientItemRepository;
import com.shantanu.request.CreateFoodRequest;
import com.shantanu.response.FoodSearchResponse;
import com.shantanu.response.TopMealResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FoodServiceImplementation implements FoodService {

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private IngredientItemRepository ingredientItemRepository;

    @Autowired
    private RestaurantService restaurantService;

    @Override
    public Food createFood(CreateFoodRequest req, Restaurant restaurant) throws Exception {
        validateFoodRequest(req);
        Category category = resolveCategory(req, restaurant);
        List<IngredientsItem> validatedIngredients = resolveIngredients(req, restaurant);

        Food food = new Food();
        food.setFoodCategory(category);
        food.setRestaurant(restaurant);
        food.setDescription(req.getDescription());
        food.setImages(req.getImages());
        food.setName(req.getName().trim());
        food.setPrice(req.getPrice());
        food.setIngredients(validatedIngredients);
        food.setSeasonal(req.getSeasonal());
        food.setVegetarian(req.getVegetarian());
        food.setAvailable(true);
        food.setCreationDate(new Date());

        Food savedFood = foodRepository.save(food);
        restaurant.getFoods().add(savedFood);

        return savedFood;
    }

    @Override
    public Food updateFood(Long foodId, CreateFoodRequest req, User actor) throws Exception {
        Food food = findFoodById(foodId);
        Restaurant restaurant = requireFoodManagementAccess(food, actor);
        validateFoodRequest(req);

        food.setFoodCategory(resolveCategory(req, restaurant));
        food.setIngredients(resolveIngredients(req, restaurant));
        food.setDescription(req.getDescription());
        food.setImages(req.getImages());
        food.setName(req.getName().trim());
        food.setPrice(req.getPrice());
        food.setSeasonal(req.getSeasonal());
        food.setVegetarian(req.getVegetarian());
        return foodRepository.save(food);
    }

    @Override
    public void deleteFood(Long foodId, User actor) throws Exception {
        Food food = findFoodById(foodId);
        requireFoodManagementAccess(food, actor);
        food.setAvailable(false);
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Food not found");
        }
        return optionalFood.get();
    }

    @Override
    public Food updateAvailabilityStatus(Long foodId, User actor) throws Exception {
        Food food = findFoodById(foodId);
        requireFoodManagementAccess(food, actor);
        food.setAvailable(!food.isAvailable());
        return foodRepository.save(food);
    }

    private Restaurant requireFoodManagementAccess(Food food, User actor) throws Exception {
        if (food.getRestaurant() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This food is not attached to a restaurant");
        }
        return restaurantService.requireRestaurantManagementAccess(food.getRestaurant().getId(), actor);
    }

    private void validateFoodRequest(CreateFoodRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Food name is required");
        }
        if (request.getPrice() == null || request.getPrice() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Food price must be greater than zero");
        }
        if (request.getCategoryId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A category is required");
        }
        if (request.getVegetarian() == null || request.getSeasonal() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vegetarian and seasonal choices are required"
            );
        }
    }

    private Category resolveCategory(CreateFoodRequest request, Restaurant restaurant) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
        requireSameRestaurant(category.getRestaurant(), restaurant, "Category");
        return category;
    }

    private List<IngredientsItem> resolveIngredients(CreateFoodRequest request, Restaurant restaurant) {
        List<IngredientsItem> validatedIngredients = new ArrayList<>();
        if (request.getIngredientIds() == null) return validatedIngredients;

        for (Long ingredientId : request.getIngredientIds()) {
            if (ingredientId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Every ingredient must have a valid id");
            }
            IngredientsItem ingredient = ingredientItemRepository.findById(ingredientId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredient not found"));
            requireSameRestaurant(ingredient.getRestaurant(), restaurant, "Ingredient");
            if (validatedIngredients.stream().noneMatch(item -> item.getId().equals(ingredient.getId()))) {
                validatedIngredients.add(ingredient);
            }
        }
        return validatedIngredients;
    }

    private void requireSameRestaurant(Restaurant relatedRestaurant, Restaurant managedRestaurant, String resourceName) {
        if (relatedRestaurant == null
                || managedRestaurant == null
                || relatedRestaurant.getId() == null
                || !relatedRestaurant.getId().equals(managedRestaurant.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    resourceName + " must belong to the managed restaurant"
            );
        }
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
