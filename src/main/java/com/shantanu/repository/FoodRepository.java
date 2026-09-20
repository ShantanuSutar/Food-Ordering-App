package com.shantanu.repository;

import com.shantanu.model.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Long> {
    List<Food> findByRestaurantId(Long restaurantId);

    boolean existsByFoodCategoryId(Long categoryId);

    boolean existsByIngredientsId(Long ingredientId);

    @Query("""
            SELECT DISTINCT f FROM Food f
            JOIN f.restaurant r
            LEFT JOIN f.foodCategory c
            WHERE f.available = true
              AND r.open = true
              AND (
                    LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY f.creationDate DESC, f.id DESC
            """)
    List<Food> searchFood(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT f FROM Food f
            WHERE f.available = true
              AND f.restaurant.open = true
            ORDER BY f.creationDate DESC, f.id DESC
            """)
    List<Food> findTopAvailableFoods(Pageable pageable);
}
