package com.shantanu.repository;

import com.shantanu.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRespository extends JpaRepository<Category, Long> {
    public List<Category> findByRestaurantId(Long id);
}
