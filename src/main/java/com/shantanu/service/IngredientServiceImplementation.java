package com.shantanu.service;

import com.shantanu.model.IngredientCategory;
import com.shantanu.model.IngredientsItem;
import com.shantanu.model.Restaurant;
import com.shantanu.model.User;
import com.shantanu.repository.IngredientCategoryRepository;
import com.shantanu.repository.IngredientItemRepository;
import com.shantanu.repository.FoodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class IngredientServiceImplementation implements IngredientsService{

    @Autowired
    private IngredientItemRepository ingredientItemRepository;

    @Autowired
    private IngredientCategoryRepository ingredientCategoryRepository;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private FoodRepository foodRepository;

    @Override
    public IngredientCategory createIngredientCategory(String name, Long restaurantId, User actor) throws Exception {
        Restaurant restaurant = restaurantService.requireRestaurantManagementAccess(restaurantId, actor);
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredient category name is required");
        }
        String normalizedName = name.trim();
        if (ingredientCategoryRepository.existsByRestaurantIdAndNameIgnoreCase(restaurantId, normalizedName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An ingredient category with this name already exists");
        }
        IngredientCategory category = new IngredientCategory();
        category.setRestaurant(restaurant);
        category.setName(normalizedName);
        return ingredientCategoryRepository.save(category);
    }

    @Override
    public IngredientCategory findIngredientCategoryById(Long id) throws Exception {
        Optional<IngredientCategory> opt = ingredientCategoryRepository.findById(id);

        if(opt.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingredient category not found");
        }

        return opt.get();
    }

    @Override
    public List<IngredientCategory> findIngredientCategoryByRestaurantId(Long id, User actor) throws Exception {
        restaurantService.requireRestaurantManagementAccess(id, actor);
        return ingredientCategoryRepository.findByRestaurantId(id);
    }

    @Override
    public IngredientsItem createIngredientItem(Long restaurantId, String ingredientName, Long categoryId, User actor) throws Exception {
        Restaurant restaurant = restaurantService.requireRestaurantManagementAccess(restaurantId, actor);
        if (ingredientName == null || ingredientName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredient name is required");
        }
        if (categoryId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredient category is required");
        }
        String normalizedName = ingredientName.trim();
        if (ingredientItemRepository.existsByRestaurantIdAndNameIgnoreCase(restaurantId, normalizedName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An ingredient with this name already exists");
        }
        IngredientCategory category = findIngredientCategoryById(categoryId);
        if (category.getRestaurant() == null
                || category.getRestaurant().getId() == null
                || !category.getRestaurant().getId().equals(restaurant.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ingredient category must belong to the managed restaurant"
            );
        }

        IngredientsItem item = new IngredientsItem();
        item.setName(normalizedName);
        item.setRestaurant(restaurant);
        item.setCategory(category);

        IngredientsItem ingredient = ingredientItemRepository.save(item);
        category.getIngredients().add(ingredient);
        return ingredient;
    }

    @Override
    public List<IngredientsItem> findRestaurantsIngredients(Long restaurantId, User actor) throws Exception {
        restaurantService.requireRestaurantManagementAccess(restaurantId, actor);
        return ingredientItemRepository.findByRestaurantId(restaurantId);
    }

    @Override
    public IngredientsItem updateStock(Long id, User actor) throws Exception {
        IngredientsItem ingredientsItem = findIngredientItemById(id);
        requireIngredientAccess(ingredientsItem, actor);
        ingredientsItem.setInStock(!ingredientsItem.isInStock());
        return ingredientItemRepository.save(ingredientsItem);
    }

    @Override
    public IngredientCategory updateIngredientCategory(Long id, String name, User actor) throws Exception {
        IngredientCategory category = findIngredientCategoryById(id);
        requireIngredientCategoryAccess(category, actor);
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredient category name is required");
        }

        String normalizedName = name.trim();
        if (!normalizedName.equalsIgnoreCase(category.getName())
                && ingredientCategoryRepository.existsByRestaurantIdAndNameIgnoreCase(
                        category.getRestaurant().getId(), normalizedName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An ingredient category with this name already exists");
        }
        category.setName(normalizedName);
        return ingredientCategoryRepository.save(category);
    }

    @Override
    public void deleteIngredientCategory(Long id, User actor) throws Exception {
        IngredientCategory category = findIngredientCategoryById(id);
        requireIngredientCategoryAccess(category, actor);
        if (ingredientItemRepository.existsByCategoryId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This category still contains ingredients and cannot be deleted"
            );
        }
        ingredientCategoryRepository.delete(category);
    }

    @Override
    public IngredientsItem updateIngredientItem(Long id, String name, Long categoryId, User actor) throws Exception {
        IngredientsItem item = findIngredientItemById(id);
        Restaurant restaurant = requireIngredientAccess(item, actor);
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredient name is required");
        }
        if (categoryId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredient category is required");
        }

        String normalizedName = name.trim();
        if (!normalizedName.equalsIgnoreCase(item.getName())
                && ingredientItemRepository.existsByRestaurantIdAndNameIgnoreCase(restaurant.getId(), normalizedName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An ingredient with this name already exists");
        }
        IngredientCategory category = findIngredientCategoryById(categoryId);
        if (category.getRestaurant() == null
                || !restaurant.getId().equals(category.getRestaurant().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ingredient category must belong to the managed restaurant"
            );
        }
        item.setName(normalizedName);
        item.setCategory(category);
        return ingredientItemRepository.save(item);
    }

    @Override
    public void deleteIngredientItem(Long id, User actor) throws Exception {
        IngredientsItem item = findIngredientItemById(id);
        requireIngredientAccess(item, actor);
        if (foodRepository.existsByIngredientsId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This ingredient is used by menu items and cannot be deleted"
            );
        }
        ingredientItemRepository.delete(item);
    }

    private IngredientsItem findIngredientItemById(Long id) {
        return ingredientItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingredient not found"));
    }

    private Restaurant requireIngredientAccess(IngredientsItem item, User actor) throws Exception {
        if (item.getRestaurant() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ingredient is not attached to a restaurant");
        }
        return restaurantService.requireRestaurantManagementAccess(item.getRestaurant().getId(), actor);
    }

    private void requireIngredientCategoryAccess(IngredientCategory category, User actor) throws Exception {
        if (category.getRestaurant() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ingredient category is not attached to a restaurant");
        }
        restaurantService.requireRestaurantManagementAccess(category.getRestaurant().getId(), actor);
    }
}
