package com.shantanu.service;

import com.shantanu.dto.RestaurantDTO;
import com.shantanu.model.Address;
import com.shantanu.model.Restaurant;
import com.shantanu.model.USER_ROLE;
import com.shantanu.model.User;
import com.shantanu.repository.AddressRepository;
import com.shantanu.repository.RestaurantRepository;
import com.shantanu.repository.UserRepository;
import com.shantanu.request.CreateRestaurantRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RestaurantServiceImplementation implements RestaurantService{

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;


    @Override
    public Restaurant createRestaurant(CreateRestaurantRequest req, User user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        if (restaurantRepository.findByOwnerId(user.getId()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This owner already has a restaurant");
        }
        validateRestaurantRequest(req);

        Address address = addressRepository.save(copyAddress(req.getAddress(), new Address()));

        Restaurant restaurant = new Restaurant();
        restaurant.setAddress(address);
        restaurant.setContactInformation(req.getContactInformation());
        restaurant.setCuisineType(req.getCuisineType());
        restaurant.setDescription(req.getDescription());
        restaurant.setImages(req.getImages());
        restaurant.setName(req.getName());
        restaurant.setOpeningHours(req.getOpeningHours());
        restaurant.setRegistrationDate(LocalDateTime.now());
        restaurant.setOwner(user);

        return restaurantRepository.save(restaurant);

    }

    @Override
    public Restaurant updateRestaurant(Long restaurantId, CreateRestaurantRequest updatedRestaurant, User actor) throws Exception {
        Restaurant restaurant = requireRestaurantManagementAccess(restaurantId, actor);
        if (updatedRestaurant == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restaurant details are required");
        }

        if (!isBlank(updatedRestaurant.getCuisineType())) {
            restaurant.setCuisineType(updatedRestaurant.getCuisineType().trim());
        }

        if (!isBlank(updatedRestaurant.getDescription())) {
            restaurant.setDescription(updatedRestaurant.getDescription().trim());
        }

        if (!isBlank(updatedRestaurant.getName())) {
            restaurant.setName(updatedRestaurant.getName().trim());
        }
        if (!isBlank(updatedRestaurant.getOpeningHours())) {
            restaurant.setOpeningHours(updatedRestaurant.getOpeningHours().trim());
        }
        if (updatedRestaurant.getImages() != null) {
            restaurant.setImages(updatedRestaurant.getImages());
        }
        if (updatedRestaurant.getContactInformation() != null) {
            restaurant.setContactInformation(updatedRestaurant.getContactInformation());
        }
        if (updatedRestaurant.getAddress() != null) {
            validateAddress(updatedRestaurant.getAddress());
            Address target = restaurant.getAddress() == null ? new Address() : restaurant.getAddress();
            restaurant.setAddress(addressRepository.save(copyAddress(updatedRestaurant.getAddress(), target)));
        }
        return restaurantRepository.save(restaurant);
    }

    @Override
    public void deleteRestaurant(Long restaurantId, User actor) throws Exception {
        requireRestaurantManagementAccess(restaurantId, actor);
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Restaurant deletion is disabled because historical orders must be preserved"
        );
    }

    @Override
    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }

    @Override
    public List<Restaurant> searchRestaurants(String keyword) {
        return restaurantRepository.findBySearchQuery(keyword);
    }

    @Override
    public Restaurant findRestaurantById(Long id) throws Exception {
        Optional<Restaurant> opt = restaurantRepository.findById(id);

        if(opt.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found");
        }

        return opt.get();
    }

    @Override
    public Restaurant getRestaurantByUserId(Long userId) throws Exception {
        Restaurant restaurant = restaurantRepository.findByOwnerId(userId);
        if(restaurant == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No restaurant exists for this owner");
        }
        return restaurant;
    }

    @Override
    public RestaurantDTO addToFavourites(Long restaurantId, User user) throws Exception {
        Restaurant restaurant = findRestaurantById(restaurantId);

        RestaurantDTO dto = new RestaurantDTO();
        dto.setDescription(restaurant.getDescription());
        dto.setImages(restaurant.getImages());
        dto.setTitle(restaurant.getName());
        dto.setId(restaurantId);

        boolean isFavourited = false;
        List<RestaurantDTO> favourites = user.getFavourites();

        for(RestaurantDTO favorite : favourites){
            if(favorite.getId().equals(restaurantId)){
                isFavourited = true;
                break;
            }
        }

        if(isFavourited){
            favourites.removeIf(favourite -> favourite.getId().equals(restaurantId));
        }else{
            favourites.add(dto);
        }

        userRepository.save(user);
        return dto;
    }

    @Override
    public Restaurant updateRestaurantStatus(Long id, User actor) throws Exception {
        Restaurant restaurant = requireRestaurantManagementAccess(id, actor);

        restaurant.setOpen(!restaurant.isOpen());
        return restaurantRepository.save(restaurant);
    }

    @Override
    public Restaurant requireRestaurantManagementAccess(Long restaurantId, User actor) throws Exception {
        Restaurant restaurant = findRestaurantById(restaurantId);
        boolean isAdministrator = actor != null && actor.getRole() == USER_ROLE.ROLE_ADMIN;
        boolean isOwner = actor != null
                && actor.getId() != null
                && restaurant.getOwner() != null
                && actor.getId().equals(restaurant.getOwner().getId());

        if (!isAdministrator && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot manage this restaurant");
        }

        return restaurant;
    }

    private void validateRestaurantRequest(CreateRestaurantRequest request) {
        if (request == null || isBlank(request.getName()) || isBlank(request.getCuisineType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restaurant name and cuisine type are required");
        }
        validateAddress(request.getAddress());
    }

    private void validateAddress(Address address) {
        if (address == null
                || isBlank(address.getStreetAddress())
                || isBlank(address.getCity())
                || isBlank(address.getState())
                || isBlank(address.getPostalCode())
                || isBlank(address.getCountry())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A complete restaurant address is required");
        }
    }

    private Address copyAddress(Address source, Address target) {
        target.setFullName(isBlank(source.getFullName()) ? null : source.getFullName().trim());
        target.setStreetAddress(source.getStreetAddress().trim());
        target.setCity(source.getCity().trim());
        target.setState(source.getState().trim());
        target.setPostalCode(source.getPostalCode().trim());
        target.setCountry(source.getCountry().trim());
        return target;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
