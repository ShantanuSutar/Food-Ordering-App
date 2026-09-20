package com.shantanu.service;

import com.shantanu.model.Address;
import com.shantanu.model.Cart;
import com.shantanu.model.CartItem;
import com.shantanu.model.Food;
import com.shantanu.model.IngredientsItem;
import com.shantanu.model.Order;
import com.shantanu.model.PaymentStatus;
import com.shantanu.model.Restaurant;
import com.shantanu.model.User;
import com.shantanu.repository.OrderItemRepository;
import com.shantanu.repository.OrderRepository;
import com.shantanu.request.AddressRequest;
import com.shantanu.request.OrderRequest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplementationTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private AddressService addressService;
    @Mock
    private RestaurantService restaurantService;
    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderServiceImplementation orderService;

    private User user;
    private Restaurant restaurant;
    private Cart cart;
    private CartItem cartItem;
    private Food food;

    @BeforeEach
    void setUp() throws Exception {
        user = new User();
        user.setId(1L);
        user.setAddresses(new ArrayList<>());

        restaurant = new Restaurant();
        restaurant.setId(2L);
        restaurant.setOpen(true);
        restaurant.setOrders(new ArrayList<>());

        food = new Food();
        food.setId(3L);
        food.setName("Paneer Bowl");
        food.setPrice(250L);
        food.setAvailable(true);
        food.setRestaurant(restaurant);
        food.setIngredients(new ArrayList<>());

        cartItem = new CartItem();
        cartItem.setFood(food);
        cartItem.setQuantity(1);
        cartItem.setTotalPrice(250L);
        cartItem.setIngredients(new ArrayList<>());

        cart = new Cart();
        cart.setCustomer(user);
        cart.setItems(new ArrayList<>());
        cart.getItems().add(cartItem);

        when(restaurantService.findRestaurantById(2L)).thenReturn(restaurant);
        lenient().when(cartService.findCartByUserId(1L)).thenReturn(cart);
        lenient().when(orderItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsOrderWithIndependentDeliveryAddressSnapshot() throws Exception {
        Address savedAddress = addressEntity(7L);
        Address snapshot = addressEntity(99L);
        when(addressService.resolveCheckoutAddress(any(), any())).thenReturn(savedAddress);
        when(addressService.createSnapshot(savedAddress)).thenReturn(snapshot);

        Order order = orderService.createOrder(orderRequest(completeAddress(7L)), user);

        assertEquals(PaymentStatus.PENDING_PAYMENT, order.getPaymentStatus());
        assertEquals(250L, order.getTotalPrice());
        assertNotSame(savedAddress, order.getDeliveryAddress());
        assertEquals(99L, order.getDeliveryAddress().getId());
        verify(addressService).createSnapshot(savedAddress);
    }

    @Test
    void rejectsCheckoutWhenCartIsEmpty() {
        cart.getItems().clear();

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> orderService.createOrder(orderRequest(completeAddress(7L)), user)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("Your cart is empty", error.getReason());
        verifyNoInteractions(addressService, orderItemRepository, orderRepository);
    }

    @Test
    void rejectsCheckoutWhenRestaurantIsClosed() {
        restaurant.setOpen(false);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> orderService.createOrder(orderRequest(completeAddress(7L)), user)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("This restaurant is currently closed", error.getReason());
        verifyNoInteractions(addressService, orderItemRepository, orderRepository);
    }

    @Test
    void rejectsCartItemsFromAnotherRestaurant() {
        Restaurant anotherRestaurant = new Restaurant();
        anotherRestaurant.setId(44L);
        food.setRestaurant(anotherRestaurant);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> orderService.createOrder(orderRequest(completeAddress(7L)), user)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("All cart items must belong to the selected restaurant", error.getReason());
        verifyNoInteractions(addressService, orderItemRepository, orderRepository);
    }

    @Test
    void rejectsFoodThatBecameUnavailable() {
        food.setAvailable(false);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> orderService.createOrder(orderRequest(completeAddress(7L)), user)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("Paneer Bowl is no longer available", error.getReason());
        verifyNoInteractions(addressService, orderItemRepository, orderRepository);
    }

    @Test
    void recalculatesOrderUsingCurrentDatabasePrice() throws Exception {
        Address savedAddress = addressEntity(7L);
        Address snapshot = addressEntity(99L);
        when(addressService.resolveCheckoutAddress(any(), any())).thenReturn(savedAddress);
        when(addressService.createSnapshot(savedAddress)).thenReturn(snapshot);
        cartItem.setQuantity(2);
        cartItem.setTotalPrice(1L);

        Order order = orderService.createOrder(orderRequest(completeAddress(7L)), user);

        assertEquals(500L, order.getTotalPrice());
        assertEquals(500L, order.getTotalAmount());
        assertEquals(2, order.getTotalItem());
        assertEquals(500L, order.getItems().get(0).getTotalPrice());
    }

    @Test
    void rejectsSelectedIngredientThatIsOutOfStock() {
        IngredientsItem ingredient = new IngredientsItem();
        ingredient.setId(8L);
        ingredient.setName("Paneer");
        ingredient.setRestaurant(restaurant);
        ingredient.setInStock(false);
        food.setIngredients(List.of(ingredient));
        cartItem.setIngredients(List.of("Paneer"));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> orderService.createOrder(orderRequest(completeAddress(7L)), user)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("A selected ingredient is no longer available", error.getReason());
        verifyNoInteractions(addressService, orderItemRepository, orderRepository);
    }

    @Test
    void resolvesAddressThroughSharedAddressService() throws Exception {
        AddressRequest requestAddress = completeAddress(null);
        Address savedAddress = addressEntity(10L);
        Address snapshot = addressEntity(11L);
        when(addressService.resolveCheckoutAddress(requestAddress, user)).thenReturn(savedAddress);
        when(addressService.createSnapshot(savedAddress)).thenReturn(snapshot);

        orderService.createOrder(orderRequest(requestAddress), user);

        verify(addressService).resolveCheckoutAddress(requestAddress, user);
        verify(addressService).createSnapshot(savedAddress);
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
