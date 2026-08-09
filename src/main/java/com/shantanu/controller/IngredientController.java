package com.shantanu.controller;

import com.shantanu.model.IngredientCategory;
import com.shantanu.model.IngredientsItem;
import com.shantanu.model.User;
import com.shantanu.request.IngredientCategoryRequest;
import com.shantanu.request.IngredientRequest;
import com.shantanu.service.IngredientsService;
import com.shantanu.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ingredients")
public class IngredientController {
    @Autowired
    private IngredientsService ingredientsService;

    @Autowired
    private UserService userService;

    @PostMapping("/category")
    public ResponseEntity<IngredientCategory> createIngredientCategory(@RequestBody IngredientCategoryRequest req, @RequestHeader("Authorization") String jwt) throws Exception {
        IngredientCategory item = ingredientsService.createIngredientCategory(req.getName(), req.getRestaurantId());
        User user = userService.findUserByJwtToken(jwt);

        return new ResponseEntity<>(item, HttpStatus.CREATED);
    }


    @PostMapping()
    public ResponseEntity<IngredientsItem> createIngredientItem(@RequestBody IngredientRequest req) throws Exception {
        IngredientsItem item = ingredientsService.createIngredientItem(req.getRestaurantId(), req.getName(), req.getCategoryId());

        return new ResponseEntity<>(item, HttpStatus.CREATED);
    }


    @PutMapping("/{id}/stock")
    public ResponseEntity<IngredientsItem> updateIngredientStock( @PathVariable Long id) throws Exception {
        IngredientsItem item = ingredientsService.updateStock(id);
        return new ResponseEntity<>(item, HttpStatus.OK);
    }

    @GetMapping("/restaurant/{id}")
    public ResponseEntity<List<IngredientsItem>> getRestaurantIngredient(@PathVariable Long id) throws Exception {
        List<IngredientsItem> items = ingredientsService.findRestaurantsIngredients(id);
        return new ResponseEntity<>(items, HttpStatus.OK);
    }

    @GetMapping("/restaurant/{id}/category")
    public ResponseEntity<List<IngredientCategory>> getRestaurantIngredientCategory(@PathVariable Long id) throws Exception {
        List<IngredientCategory> items = ingredientsService.findIngredientCategoryByRestaurantId(id);
        return new ResponseEntity<>(items, HttpStatus.OK);
    }

}
