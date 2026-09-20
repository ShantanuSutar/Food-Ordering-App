package com.shantanu.service;

import com.shantanu.model.Category;
import com.shantanu.model.Food;
import com.shantanu.model.Restaurant;
import com.shantanu.model.User;
import com.shantanu.repository.CategoryRepository;
import com.shantanu.repository.FoodRepository;
import com.shantanu.repository.IngredientItemRepository;
import com.shantanu.request.CreateFoodRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FoodServiceAuthorizationTest {

    @Mock
    private FoodRepository foodRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private IngredientItemRepository ingredientItemRepository;
    @Mock
    private RestaurantService restaurantService;

    @InjectMocks
    private FoodServiceImplementation foodService;

    private Restaurant restaurant;
    private Food food;
    private User actor;

    @BeforeEach
    void setUp() {
        restaurant = new Restaurant();
        restaurant.setId(10L);
        restaurant.setFoods(new ArrayList<>());
        food = new Food();
        food.setId(20L);
        food.setRestaurant(restaurant);
        food.setAvailable(true);
        actor = new User();
        actor.setId(1L);
    }

    @Test
    void refusesAvailabilityChangeWhenActorDoesNotOwnRestaurant() throws Exception {
        when(foodRepository.findById(20L)).thenReturn(Optional.of(food));
        when(restaurantService.requireRestaurantManagementAccess(10L, actor))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> foodService.updateAvailabilityStatus(20L, actor)
        );

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(foodRepository, never()).save(any());
    }

    @Test
    void foodDeleteArchivesWithoutDetachingHistoricalRelationship() throws Exception {
        when(foodRepository.findById(20L)).thenReturn(Optional.of(food));
        when(restaurantService.requireRestaurantManagementAccess(10L, actor)).thenReturn(restaurant);

        foodService.deleteFood(20L, actor);

        assertFalse(food.isAvailable());
        assertSame(restaurant, food.getRestaurant());
        verify(foodRepository).save(food);
    }

    @Test
    void rejectsCategoryFromAnotherRestaurant() {
        Restaurant otherRestaurant = new Restaurant();
        otherRestaurant.setId(11L);
        Category category = new Category();
        category.setId(30L);
        category.setRestaurant(otherRestaurant);
        when(categoryRepository.findById(30L)).thenReturn(Optional.of(category));

        CreateFoodRequest request = new CreateFoodRequest();
        request.setName("Rice bowl");
        request.setPrice(250L);
        request.setVegetarian(true);
        request.setSeasonal(false);
        request.setCategoryId(30L);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> foodService.createFood(request, restaurant)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(foodRepository, never()).save(any());
    }

    @Test
    void newlyCreatedFoodIsAvailableByDefault() throws Exception {
        Category category = new Category();
        category.setId(30L);
        category.setRestaurant(restaurant);
        when(categoryRepository.findById(30L)).thenReturn(Optional.of(category));
        when(foodRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateFoodRequest request = new CreateFoodRequest();
        request.setName("Rice bowl");
        request.setPrice(250L);
        request.setVegetarian(true);
        request.setSeasonal(false);
        request.setCategoryId(30L);

        Food created = foodService.createFood(request, restaurant);

        assertTrue(created.isAvailable());
    }

    @Test
    void ownerCanEditFoodWithoutChangingAvailability() throws Exception {
        Category category = new Category();
        category.setId(30L);
        category.setRestaurant(restaurant);
        when(foodRepository.findById(20L)).thenReturn(Optional.of(food));
        when(restaurantService.requireRestaurantManagementAccess(10L, actor)).thenReturn(restaurant);
        when(categoryRepository.findById(30L)).thenReturn(Optional.of(category));
        when(foodRepository.save(food)).thenReturn(food);

        CreateFoodRequest request = new CreateFoodRequest();
        request.setName("Updated bowl");
        request.setDescription("Updated description");
        request.setPrice(300L);
        request.setVegetarian(true);
        request.setSeasonal(false);
        request.setCategoryId(30L);

        Food updated = foodService.updateFood(20L, request, actor);

        assertEquals("Updated bowl", updated.getName());
        assertEquals(300L, updated.getPrice());
        assertTrue(updated.isAvailable());
    }
}
