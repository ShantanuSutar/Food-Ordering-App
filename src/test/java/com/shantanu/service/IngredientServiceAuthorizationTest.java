package com.shantanu.service;

import com.shantanu.model.IngredientCategory;
import com.shantanu.model.IngredientsItem;
import com.shantanu.model.Restaurant;
import com.shantanu.model.User;
import com.shantanu.repository.IngredientCategoryRepository;
import com.shantanu.repository.IngredientItemRepository;
import com.shantanu.repository.FoodRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngredientServiceAuthorizationTest {

    @Mock
    private IngredientItemRepository ingredientItemRepository;
    @Mock
    private IngredientCategoryRepository ingredientCategoryRepository;
    @Mock
    private RestaurantService restaurantService;
    @Mock
    private FoodRepository foodRepository;

    @InjectMocks
    private IngredientServiceImplementation ingredientService;

    @Test
    void preventsStockChangeForAnotherOwnersIngredient() throws Exception {
        User actor = new User();
        actor.setId(4L);
        Restaurant restaurant = new Restaurant();
        restaurant.setId(10L);
        IngredientsItem ingredient = new IngredientsItem();
        ingredient.setId(30L);
        ingredient.setRestaurant(restaurant);
        when(ingredientItemRepository.findById(30L)).thenReturn(Optional.of(ingredient));
        when(restaurantService.requireRestaurantManagementAccess(10L, actor))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> ingredientService.updateStock(30L, actor)
        );

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(ingredientItemRepository, never()).save(any());
    }

    @Test
    void rejectsIngredientCategoryFromAnotherRestaurant() throws Exception {
        User actor = new User();
        actor.setId(4L);
        Restaurant restaurant = new Restaurant();
        restaurant.setId(10L);
        Restaurant otherRestaurant = new Restaurant();
        otherRestaurant.setId(11L);
        IngredientCategory category = new IngredientCategory();
        category.setId(40L);
        category.setRestaurant(otherRestaurant);
        when(restaurantService.requireRestaurantManagementAccess(10L, actor)).thenReturn(restaurant);
        when(ingredientCategoryRepository.findById(40L)).thenReturn(Optional.of(category));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> ingredientService.createIngredientItem(10L, "Cheese", 40L, actor)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(ingredientItemRepository, never()).save(any());
    }

    @Test
    void preventsDeletingIngredientUsedByMenuItems() throws Exception {
        User actor = new User();
        Restaurant restaurant = new Restaurant();
        restaurant.setId(10L);
        IngredientsItem ingredient = new IngredientsItem();
        ingredient.setId(30L);
        ingredient.setRestaurant(restaurant);
        when(ingredientItemRepository.findById(30L)).thenReturn(Optional.of(ingredient));
        when(restaurantService.requireRestaurantManagementAccess(10L, actor)).thenReturn(restaurant);
        when(foodRepository.existsByIngredientsId(30L)).thenReturn(true);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> ingredientService.deleteIngredientItem(30L, actor)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(ingredientItemRepository, never()).delete(any());
    }
}
