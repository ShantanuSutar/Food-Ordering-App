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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceAuthorizationTest {

    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RestaurantServiceImplementation restaurantService;

    private Restaurant restaurant;
    private User owner;

    @BeforeEach
    void setUp() {
        owner = user(1L, USER_ROLE.ROLE_RESTAURANT_OWNER);
        restaurant = new Restaurant();
        restaurant.setId(10L);
        restaurant.setOwner(owner);
        restaurant.setOpen(true);
        lenient().when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
    }

    @Test
    void preventsOneOwnerFromChangingAnotherOwnersRestaurant() {
        User otherOwner = user(2L, USER_ROLE.ROLE_RESTAURANT_OWNER);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> restaurantService.updateRestaurantStatus(10L, otherOwner)
        );

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void permitsTheOwnerToChangeRestaurantStatus() throws Exception {
        when(restaurantRepository.save(restaurant)).thenReturn(restaurant);

        Restaurant updated = restaurantService.updateRestaurantStatus(10L, owner);

        assertFalse(updated.isOpen());
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    void permitsAdministratorManagementAccess() throws Exception {
        User administrator = user(99L, USER_ROLE.ROLE_ADMIN);

        Restaurant result = restaurantService.requireRestaurantManagementAccess(10L, administrator);

        assertEquals(restaurant, result);
    }

    @Test
    void blocksHardDeleteEvenForTheOwner() {
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> restaurantService.deleteRestaurant(10L, owner)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(restaurantRepository, never()).delete(any());
    }

    @Test
    void preventsOwnerFromCreatingASecondRestaurant() {
        when(restaurantRepository.findByOwnerId(1L)).thenReturn(restaurant);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> restaurantService.createRestaurant(new CreateRestaurantRequest(), owner)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(addressRepository, never()).save(any());
    }

    @Test
    void reportsMissingOwnerRestaurantAsNotFound() {
        when(restaurantRepository.findByOwnerId(1L)).thenReturn(null);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> restaurantService.getRestaurantByUserId(1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void favouriteToggleReturnsTheCurrentRestaurantDetails() throws Exception {
        restaurant.setName("Open Kitchen");
        Address address = new Address();
        address.setCity("Pune");
        restaurant.setAddress(address);

        Restaurant result = restaurantService.addToFavourites(10L, owner);

        assertSame(restaurant, result);
        assertEquals(1, owner.getFavourites().size());
        assertEquals("Open Kitchen", owner.getFavourites().get(0).getTitle());
        verify(userRepository).save(owner);
    }

    @Test
    void resolvesSavedFavouriteIdsToFreshRestaurantRecords() {
        RestaurantDTO savedFavourite = new RestaurantDTO();
        savedFavourite.setId(10L);
        savedFavourite.setTitle("Old title");
        owner.setFavourites(new ArrayList<>(List.of(savedFavourite)));
        restaurant.setName("Current title");
        restaurant.setOpen(true);
        when(restaurantRepository.findAllById(List.of(10L))).thenReturn(List.of(restaurant));

        List<Restaurant> favourites = restaurantService.getFavouriteRestaurants(owner);

        assertEquals(1, favourites.size());
        assertEquals("Current title", favourites.get(0).getName());
        assertTrue(favourites.get(0).isOpen());
    }

    private User user(Long id, USER_ROLE role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }
}
