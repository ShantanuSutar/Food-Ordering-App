package com.shantanu.service;

import com.shantanu.model.Cart;
import com.shantanu.model.CartItem;
import com.shantanu.model.Food;
import com.shantanu.model.User;
import com.shantanu.repository.CartItemRepository;
import com.shantanu.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceAuthorizationTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private UserService userService;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private FoodService foodService;

    @InjectMocks
    private CartServiceImplementation cartService;

    private User user;

    @BeforeEach
    void setUp() throws Exception {
        user = new User();
        user.setId(4L);
        lenient().when(userService.findUserByJwtToken("Bearer token")).thenReturn(user);
    }

    @Test
    void cannotUpdateAnotherUsersCartItem() {
        when(cartItemRepository.findByIdAndCart_Customer_Id(55L, 4L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> cartService.updateCartItemQuantity(55L, 2, "Bearer token")
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void rejectsZeroQuantityBeforeMutation() {
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> cartService.updateCartItemQuantity(55L, 0, "Bearer token")
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void rejectsExcessiveQuantityBeforeMutation() {
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> cartService.updateCartItemQuantity(55L, 100, "Bearer token")
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(cartItemRepository);
    }

    @Test
    void updatesAnOwnedCartItem() throws Exception {
        Food food = new Food();
        food.setId(7L);
        food.setPrice(125L);
        CartItem item = new CartItem();
        item.setId(55L);
        item.setFood(food);
        Cart cart = new Cart();
        cart.setItems(new ArrayList<>());
        cart.getItems().add(item);
        item.setCart(cart);
        when(cartItemRepository.findByIdAndCart_Customer_Id(55L, 4L)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(item)).thenReturn(item);

        CartItem result = cartService.updateCartItemQuantity(55L, 3, "Bearer token");

        assertEquals(3, result.getQuantity());
        assertEquals(375L, result.getTotalPrice());
        assertEquals(375L, cart.getTotal());
        verify(cartRepository).save(cart);
    }

    @Test
    void cannotRemoveAnotherUsersCartItem() throws Exception {
        Cart cart = new Cart();
        cart.setItems(new ArrayList<>());
        when(cartRepository.findByCustomerId(4L)).thenReturn(cart);
        when(cartItemRepository.findByIdAndCart_Customer_Id(55L, 4L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> cartService.removeItemFromCart(55L, "Bearer token")
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(cartRepository, never()).save(any());
    }
}
