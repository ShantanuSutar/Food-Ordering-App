package com.shantanu.service;

import com.shantanu.model.Category;
import com.shantanu.model.User;

import java.util.List;

public interface CategoryService {
    public Category createCategory(String name, Long userId) throws Exception;

    public List<Category> findCategoryByRestaurantId(Long id) throws Exception;

    public Category findCategoryById(Long id) throws Exception;

    Category updateCategory(Long id, String name, User actor) throws Exception;

    void deleteCategory(Long id, User actor) throws Exception;
}
