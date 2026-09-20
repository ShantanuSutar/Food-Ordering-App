package com.shantanu.service;

import com.shantanu.model.IngredientCategory;
import com.shantanu.model.IngredientsItem;
import com.shantanu.model.User;

import java.util.List;

public interface IngredientsService {
    public IngredientCategory createIngredientCategory(String name, Long restaurantId, User actor) throws Exception;

    public IngredientCategory findIngredientCategoryById(Long id) throws Exception;

    public List<IngredientCategory> findIngredientCategoryByRestaurantId(Long id, User actor) throws Exception;

    public IngredientsItem createIngredientItem(Long restaurantId, String ingredientName, Long categoryId, User actor) throws Exception;

    public List<IngredientsItem> findRestaurantsIngredients(Long restaurantId, User actor) throws Exception;

    public IngredientsItem updateStock(Long id, User actor) throws Exception;

    IngredientCategory updateIngredientCategory(Long id, String name, User actor) throws Exception;

    void deleteIngredientCategory(Long id, User actor) throws Exception;

    IngredientsItem updateIngredientItem(Long id, String name, Long categoryId, User actor) throws Exception;

    void deleteIngredientItem(Long id, User actor) throws Exception;
}
