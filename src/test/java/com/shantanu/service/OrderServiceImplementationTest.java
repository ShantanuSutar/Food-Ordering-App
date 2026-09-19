package com.shantanu.service;

import com.shantanu.model.Address;
import com.shantanu.model.Cart;
import com.shantanu.model.CartItem;
import com.shantanu.model.Order;
import com.shantanu.model.Restaurant;
import com.shantanu.model.User;
import com.shantanu.repository.AddressRepository;
import com.shantanu.repository.OrderItemRepository;
import com.shantanu.repository.OrderRepository;
import com.shantanu.repository.UserRepository;
import com.shantanu.request.AddressRequest;
import com.shantanu.request.OrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplementationTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RestaurantService restaurantService;
    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderServiceImplementation orderService;

    private User user;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() throws Exception {
        user = new User();
        user.setId(1L);
        user.setAddresses(new ArrayList<>());

        restaurant = new Restaurant();
        restaurant.setId(2L);
        restaurant.setOrders(new ArrayList<>());

        CartItem cartItem = new CartItem();
        cartItem.setQuantity(1);
        cartItem.setTotalPrice(250L);

        Cart cart = new Cart();
        cart.setItems(new ArrayList<>());
        cart.getItems().add(cartItem);

        when(restaurantService.findRestaurantById(2L)).thenReturn(restaurant);
        when(cartService.findCartByUserId(1L)).thenReturn(cart);
        when(cartService.calculateCartTotals(cart)).thenReturn(250L);
        when(orderItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void savesNewCanonicalAddressAndAddsItToUser() throws Exception {
        when(addressRepository.save(any())).thenAnswer(invocation -> {
            Address address = invocation.getArgument(0);
            address.setId(10L);
            return address;
        });

        Order order = orderService.createOrder(orderRequest(completeAddress(null)), user);

        assertEquals(1, user.getAddresses().size());
        assertEquals("Maharashtra", order.getDeliveryAddress().getState());
        assertEquals("411001", order.getDeliveryAddress().getPostalCode());
        verify(userRepository).save(user);
    }

    @Test
    void reusesAnIdenticalSavedAddressIgnoringCaseAndWhitespace() throws Exception {
        Address existing = addressEntity(7L);
        user.getAddresses().add(existing);
        AddressRequest duplicate = completeAddress(null);
        duplicate.setCity(" pune ");
        duplicate.setState("MAHARASHTRA");

        Order order = orderService.createOrder(orderRequest(duplicate), user);

        assertSame(existing, order.getDeliveryAddress());
        verify(addressRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updatesAnOwnedLegacyAddressBeforeCheckout() throws Exception {
        Address legacy = new Address();
        legacy.setId(7L);
        legacy.setCity("Pune");
        legacy.setState("Maharashtra");
        legacy.setPostalCode("411001");
        legacy.setCountry("India");
        user.getAddresses().add(legacy);
        when(addressRepository.save(legacy)).thenReturn(legacy);

        Order order = orderService.createOrder(orderRequest(completeAddress(7L)), user);

        assertSame(legacy, order.getDeliveryAddress());
        assertEquals("Asha Patil", legacy.getFullName());
        assertEquals("12 Market Road", legacy.getStreetAddress());
        verify(addressRepository).save(legacy);
    }

    private OrderRequest orderRequest(AddressRequest address) {
        OrderRequest request = new OrderRequest();
        request.setRestaurantId(2L);
        request.setDeliveryAddress(address);
        return request;
    }

    private AddressRequest completeAddress(Long id) {
        AddressRequest address = new AddressRequest();
        address.setId(id);
        address.setFullName("Asha Patil");
        address.setStreetAddress("12 Market Road");
        address.setCity("Pune");
        address.setState("Maharashtra");
        address.setPostalCode("411001");
        address.setCountry("India");
        return address;
    }

    private Address addressEntity(Long id) {
        AddressRequest request = completeAddress(id);
        Address address = new Address();
        address.setId(request.getId());
        address.setFullName(request.getFullName());
        address.setStreetAddress(request.getStreetAddress());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        return address;
    }
}
