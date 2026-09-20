package com.shantanu.controller;

import com.shantanu.model.IngredientCategory;
import com.shantanu.model.IngredientsItem;
import com.shantanu.model.User;
import com.shantanu.request.IngredientCategoryRequest;
import com.shantanu.request.IngredientRequest;
import com.shantanu.response.MessageResponse;
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
        User user = userService.findUserByJwtToken(jwt);
        IngredientCategory item = ingredientsService.createIngredientCategory(req.getName(), req.getRestaurantId(), user);

        return new ResponseEntity<>(item, HttpStatus.CREATED);
    }


    @PostMapping()
    public ResponseEntity<IngredientsItem> createIngredientItem(@RequestBody IngredientRequest req, @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserByJwtToken(jwt);
        IngredientsItem item = ingredientsService.createIngredientItem(req.getRestaurantId(), req.getName(), req.getCategoryId(), user);

        return new ResponseEntity<>(item, HttpStatus.CREATED);
    }


    @PutMapping("/{id}/stock")
    public ResponseEntity<IngredientsItem> updateIngredientStock( @PathVariable Long id, @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserByJwtToken(jwt);
        IngredientsItem item = ingredientsService.updateStock(id, user);
        return new ResponseEntity<>(item, HttpStatus.OK);
    }

    @PutMapping("/category/{id}")
    public ResponseEntity<IngredientCategory> updateIngredientCategory(
            @PathVariable Long id,
            @RequestBody IngredientCategoryRequest req,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserByJwtToken(jwt);
        return ResponseEntity.ok(ingredientsService.updateIngredientCategory(id, req.getName(), user));
    }

    @DeleteMapping("/category/{id}")
    public ResponseEntity<MessageResponse> deleteIngredientCategory(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserByJwtToken(jwt);
        ingredientsService.deleteIngredientCategory(id, user);
        MessageResponse response = new MessageResponse();
        response.setMessage("Ingredient category deleted");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IngredientsItem> updateIngredientItem(
            @PathVariable Long id,
            @RequestBody IngredientRequest req,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserByJwtToken(jwt);
        return ResponseEntity.ok(ingredientsService.updateIngredientItem(
                id, req.getName(), req.getCategoryId(), user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteIngredientItem(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserByJwtToken(jwt);
        ingredientsService.deleteIngredientItem(id, user);
        MessageResponse response = new MessageResponse();
        response.setMessage("Ingredient deleted");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/restaurant/{id}")
    public ResponseEntity<List<IngredientsItem>> getRestaurantIngredient(@PathVariable Long id, @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserByJwtToken(jwt);
        List<IngredientsItem> items = ingredientsService.findRestaurantsIngredients(id, user);
        return new ResponseEntity<>(items, HttpStatus.OK);
    }

    @GetMapping("/restaurant/{id}/category")
    public ResponseEntity<List<IngredientCategory>> getRestaurantIngredientCategory(@PathVariable Long id, @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserByJwtToken(jwt);
        List<IngredientCategory> items = ingredientsService.findIngredientCategoryByRestaurantId(id, user);
        return new ResponseEntity<>(items, HttpStatus.OK);
    }

}
