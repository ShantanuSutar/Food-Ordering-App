package com.shantanu.service;

import com.shantanu.model.Category;
import com.shantanu.model.Restaurant;
import com.shantanu.model.User;
import com.shantanu.repository.CategoryRepository;
import com.shantanu.repository.FoodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImplementation implements CategoryService{

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FoodRepository foodRepository;

    @Override
    public Category createCategory(String name, Long userId) throws Exception {
        Restaurant restaurant = restaurantService.getRestaurantByUserId(userId);
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name is required");
        }
        String normalizedName = name.trim();
        if (categoryRepository.existsByRestaurantIdAndNameIgnoreCase(restaurant.getId(), normalizedName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A category with this name already exists");
        }
        Category category = new Category();
        category.setName(normalizedName);
        category.setRestaurant(restaurant);
        return categoryRepository.save(category);
    }

    @Override
    public List<Category> findCategoryByRestaurantId(Long id) throws Exception {
        restaurantService.findRestaurantById(id);
        return categoryRepository.findByRestaurantId(id);
    }

    @Override
    public Category findCategoryById(Long id) throws Exception {
        Optional<Category> optionalCategory = categoryRepository.findById(id);

        if(optionalCategory.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        return optionalCategory.get();
    }

    @Override
    public Category updateCategory(Long id, String name, User actor) throws Exception {
        Category category = findCategoryById(id);
        requireCategoryAccess(category, actor);
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name is required");
        }

        String normalizedName = name.trim();
        if (!normalizedName.equalsIgnoreCase(category.getName())
                && categoryRepository.existsByRestaurantIdAndNameIgnoreCase(category.getRestaurant().getId(), normalizedName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A category with this name already exists");
        }
        category.setName(normalizedName);
        return categoryRepository.save(category);
    }

    @Override
    public void deleteCategory(Long id, User actor) throws Exception {
        Category category = findCategoryById(id);
        requireCategoryAccess(category, actor);
        if (foodRepository.existsByFoodCategoryId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This category is used by menu items and cannot be deleted"
            );
        }
        categoryRepository.delete(category);
    }

    private void requireCategoryAccess(Category category, User actor) throws Exception {
        if (category.getRestaurant() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category is not attached to a restaurant");
        }
        restaurantService.requireRestaurantManagementAccess(category.getRestaurant().getId(), actor);
    }
}
