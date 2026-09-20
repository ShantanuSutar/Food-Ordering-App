package com.shantanu.service;

import com.shantanu.model.Order;
import com.shantanu.model.User;
import com.shantanu.request.OrderRequest;

import java.util.List;

public interface OrderService {
    public Order createOrder(OrderRequest order, User user) throws Exception;

    public Order updateOrder(Long orderId, String orderStatus, User actor) throws Exception;

    public Order cancelOrder(Long orderId, User actor) throws Exception;

    public List<Order> getUsersOrder(Long userId) throws Exception;

    public List<Order> getRestaurantsOrder(Long restaurantId, String orderStatus, User actor) throws Exception;

    public Order findOrderById (Long orderId) throws Exception;

    Order findUsersOrderById(Long orderId, Long userId) throws Exception;
}
