package com.shantanu.controller;

import com.shantanu.dto.RestaurantDTO;
import com.shantanu.model.Restaurant;
import com.shantanu.model.User;
import com.shantanu.request.CreateRestaurantRequest;
import com.shantanu.service.RestaurantService;
import com.shantanu.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {
    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private UserService userService;

    @GetMapping("/search")
    public ResponseEntity<List<Restaurant>> createRestaurant( @RequestHeader("Authorization") String jwt, @RequestParam String keyword) throws Exception {
        User user= userService.findUserByJwtToken(jwt);

        List<Restaurant> restaurant = restaurantService.searchRestaurants(keyword);
        return new ResponseEntity<>(restaurant, HttpStatus.OK);
    }

    @GetMapping()
    public ResponseEntity<List<Restaurant>> getAllRestaurant(@RequestHeader("Authorization") String jwt) throws Exception {
        User user= userService.findUserByJwtToken(jwt);

        List<Restaurant> restaurant = restaurantService.getAllRestaurants();
        return new ResponseEntity<>(restaurant, HttpStatus.OK);
    }


    @GetMapping("/{id}")
    public ResponseEntity<Restaurant> findRestaurantById(@RequestHeader("Authorization") String jwt, @PathVariable Long id) throws Exception {
        User user= userService.findUserByJwtToken(jwt);

        Restaurant restaurant = restaurantService.findRestaurantById(id);
        return new ResponseEntity<>(restaurant, HttpStatus.OK);
    }


    @PutMapping("/{id}/add-favourites")
    public ResponseEntity<RestaurantDTO> addToFavourites(@RequestHeader("Authorization") String jwt, @PathVariable Long id) throws Exception {
        User user= userService.findUserByJwtToken(jwt);

        RestaurantDTO restaurant = restaurantService.addToFavourites(id, user);
        return new ResponseEntity<>(restaurant, HttpStatus.OK);
    }

}
