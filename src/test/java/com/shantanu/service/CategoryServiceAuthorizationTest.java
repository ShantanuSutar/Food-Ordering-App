package com.shantanu.service;

import com.shantanu.model.Category;
import com.shantanu.model.Restaurant;
import com.shantanu.model.User;
import com.shantanu.repository.CategoryRepository;
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
class CategoryServiceAuthorizationTest {

    @Mock
    private RestaurantService restaurantService;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private FoodRepository foodRepository;

    @InjectMocks
    private CategoryServiceImplementation categoryService;

    @Test
    void preventsAnotherOwnerFromRenamingCategory() throws Exception {
        User actor = new User();
        actor.setId(8L);
        Restaurant restaurant = new Restaurant();
        restaurant.setId(10L);
        Category category = category(20L, "Mains", restaurant);
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));
        when(restaurantService.requireRestaurantManagementAccess(10L, actor))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> categoryService.updateCategory(20L, "Dinner", actor)
        );

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void preventsDeletingCategoryUsedByMenuItems() throws Exception {
        User actor = new User();
        Restaurant restaurant = new Restaurant();
        restaurant.setId(10L);
        Category category = category(20L, "Mains", restaurant);
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(category));
        when(restaurantService.requireRestaurantManagementAccess(10L, actor)).thenReturn(restaurant);
        when(foodRepository.existsByFoodCategoryId(20L)).thenReturn(true);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> categoryService.deleteCategory(20L, actor)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(categoryRepository, never()).delete(any());
    }

    private Category category(Long id, String name, Restaurant restaurant) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setRestaurant(restaurant);
        return category;
    }
}
