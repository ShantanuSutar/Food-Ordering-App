package com.shantanu.service;

import com.shantanu.model.Order;
import com.shantanu.model.PaymentStatus;
import com.shantanu.model.Restaurant;
import com.shantanu.model.User;
import com.shantanu.repository.OrderItemRepository;
import com.shantanu.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderAuthorizationTest {

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

    @Test
    void preventsOwnerFromUpdatingAnotherRestaurantsOrder() throws Exception {
        User actor = new User();
        actor.setId(4L);
        Restaurant restaurant = new Restaurant();
        restaurant.setId(10L);
        Order order = new Order();
        order.setId(50L);
        order.setRestaurant(restaurant);
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(restaurantService.requireRestaurantManagementAccess(10L, actor))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> orderService.updateOrder(50L, "DELIVERED", actor)
        );

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void permitsTheNextStatusForAPaidOrder() throws Exception {
        User actor = new User();
        actor.setId(4L);
        Restaurant restaurant = new Restaurant();
        restaurant.setId(10L);
        Order order = new Order();
        order.setId(50L);
        order.setRestaurant(restaurant);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setOrderStatus("CONFIRMED");
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order updated = orderService.updateOrder(50L, "PREPARING", actor);

        assertEquals("PREPARING", updated.getOrderStatus());
        verify(restaurantService).requireRestaurantManagementAccess(10L, actor);
    }

    @Test
    void preventsInvalidBackwardOrderTransition() throws Exception {
        User actor = new User();
        actor.setId(4L);
        Restaurant restaurant = new Restaurant();
        restaurant.setId(10L);
        Order order = new Order();
        order.setId(50L);
        order.setRestaurant(restaurant);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setOrderStatus("DELIVERED");
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> orderService.updateOrder(50L, "PREPARING", actor)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void preventsUnpaidOrderFromEnteringFulfilment() throws Exception {
        User actor = new User();
        actor.setId(4L);
        Restaurant restaurant = new Restaurant();
        restaurant.setId(10L);
        Order order = new Order();
        order.setId(50L);
        order.setRestaurant(restaurant);
        order.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        order.setOrderStatus("PENDING");
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> orderService.updateOrder(50L, "CONFIRMED", actor)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(orderRepository, never()).save(any());
    }
}
