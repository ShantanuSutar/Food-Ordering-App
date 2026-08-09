package com.shantanu.service;

import com.shantanu.model.Category;
import com.shantanu.model.Restaurant;
import com.shantanu.repository.CategoryRespository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImplementation implements CategoryService{

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private CategoryRespository categoryRespository;

    @Override
    public Category createCategory(String name, Long userId) throws Exception {
        Restaurant restaurant = restaurantService.getRestaurantByUserId(userId);
        Category category = new Category();
        category.setName(name);
        category.setRestaurant(restaurant);
        return categoryRespository.save(category);
    }

    @Override
    public List<Category> findCategoryByRestaurantId(Long id) throws Exception {
        Restaurant restaurant = restaurantService.getRestaurantByUserId(id);
        return categoryRespository.findByRestaurantId(restaurant.getId());
    }

    @Override
    public Category findCategoryById(Long id) throws Exception {
        Optional<Category> optionalCategory = categoryRespository.findById(id);

        if(optionalCategory.isEmpty()){
            throw new Exception("Category not found!");
        }
        return optionalCategory.get();
    }
}
