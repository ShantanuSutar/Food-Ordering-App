package com.shantanu.repository;

import com.shantanu.model.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Long> {
    List<Food> findByRestaurantId(Long restaurantId);

    @Query("SELECT f FROM Food f WHERE f.name LIKE %:keyword% OR f.foodCategory.name LIKE %:keyword%")
    List<Food> searchFood(@Param("keyword") String keyword);

    @Query("""
            SELECT f FROM Food f
            WHERE f.available = true
              AND f.restaurant.open = true
            ORDER BY f.creationDate DESC, f.id DESC
            """)
    List<Food> findTopAvailableFoods(Pageable pageable);
}
