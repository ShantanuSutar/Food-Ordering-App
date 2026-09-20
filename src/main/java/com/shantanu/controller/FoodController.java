package com.shantanu.controller;

import com.shantanu.model.Food;
import com.shantanu.response.FoodSearchResponse;
import com.shantanu.response.TopMealResponse;
import com.shantanu.service.FoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/food")
public class FoodController {

    @Autowired
    private FoodService foodService;

    @GetMapping("/top")
    public ResponseEntity<List<TopMealResponse>> getTopMeals(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return new ResponseEntity<>(foodService.getTopMeals(limit), HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<List<FoodSearchResponse>> searchFood(@RequestParam String name) {
        List<FoodSearchResponse> foods = foodService.searchFood(name);

        return new ResponseEntity<>(foods, HttpStatus.OK);
    }


    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<Food>> getRestaurantFood(
            @RequestParam(required = false) boolean vegetarian,
            @RequestParam(required = false) boolean seasonal,
            @RequestParam(required = false) boolean nonveg,
            @RequestParam(required = false) String foodCategory,
            @PathVariable Long restaurantId
    ) {
        List<Food> foods = foodService.getRestaurantsFood(
                restaurantId,
                vegetarian,
                nonveg,
                seasonal,
                foodCategory
        );

        return new ResponseEntity<>(foods, HttpStatus.OK);
    }

}
