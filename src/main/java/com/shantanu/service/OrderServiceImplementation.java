package com.shantanu.service;

import com.shantanu.model.*;
import com.shantanu.repository.OrderItemRepository;
import com.shantanu.repository.OrderRepository;
import com.shantanu.request.OrderRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderServiceImplementation implements OrderService {

    private static final int MAX_CART_ITEM_QUANTITY = 99;
    private static final Map<String, Set<String>> ORDER_TRANSITIONS = Map.of(
            "PENDING", Set.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED", Set.of("PREPARING", "CANCELLED"),
            "PREPARING", Set.of("READY", "CANCELLED"),
            "READY", Set.of("OUT_FOR_DELIVERY", "CANCELLED"),
            "OUT_FOR_DELIVERY", Set.of("DELIVERED"),
            "DELIVERED", Set.of(),
            "COMPLETED", Set.of(),
            "CANCELLED", Set.of()
    );

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private AddressService addressService;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private CartService cartService;

    @Autowired
    private PaymentService paymentService;

    @Override
    @Transactional
    public Order createOrder(OrderRequest order, User user) throws Exception {
        if (order == null || order.getRestaurantId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid restaurant is required");
        }
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        Restaurant restaurant = restaurantService.findRestaurantById(order.getRestaurantId());
        if (!restaurant.isOpen()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This restaurant is currently closed");
        }

        Cart cart = cartService.findCartByUserId(user.getId());
        CheckoutSummary checkout = validateCheckoutCart(cart, restaurant);

        Address savedAddress = addressService.resolveCheckoutAddress(order.getDeliveryAddress(), user);
        Address deliveryAddressSnapshot = addressService.createSnapshot(savedAddress);

        Order createdOrder = new Order();
        createdOrder.setCustomer(user);
        createdOrder.setCreatedAt(new Date());
        createdOrder.setOrderStatus("PENDING");
        createdOrder.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        createdOrder.setDeliveryAddress(deliveryAddressSnapshot);
        createdOrder.setRestaurant(restaurant);

        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItem orderItem : checkout.items()) {
            OrderItem savedOrderItem = orderItemRepository.save(orderItem);
            orderItems.add(savedOrderItem);
        }

        createdOrder.setItems(orderItems);
        createdOrder.setTotalItem(checkout.totalItems());
        createdOrder.setTotalPrice(checkout.totalPrice());
        createdOrder.setTotalAmount(checkout.totalPrice());

        Order savedOrder = orderRepository.save(createdOrder);
        if (restaurant.getOrders() == null) {
            restaurant.setOrders(new ArrayList<>());
        }
        restaurant.getOrders().add(savedOrder);

        return savedOrder;
    }

    private CheckoutSummary validateCheckoutCart(Cart cart, Restaurant restaurant) {
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        long totalPrice = 0L;
        int totalItems = 0;

        try {
            for (CartItem cartItem : cart.getItems()) {
                Food food = cartItem == null ? null : cartItem.getFood();
                validateCheckoutFood(food, restaurant);
                validateCheckoutQuantity(cartItem.getQuantity());

                long lineTotal = Math.multiplyExact(food.getPrice(), (long) cartItem.getQuantity());
                totalPrice = Math.addExact(totalPrice, lineTotal);
                totalItems = Math.addExact(totalItems, cartItem.getQuantity());

                OrderItem orderItem = new OrderItem();
                orderItem.setFood(food);
                orderItem.setItemName(food.getName());
                orderItem.setUnitPrice(food.getPrice());
                orderItem.setIngredients(validateSelectedIngredients(cartItem.getIngredients(), food, restaurant));
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setTotalPrice(lineTotal);
                orderItems.add(orderItem);
            }
        } catch (ArithmeticException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart total is too large");
        }

        return new CheckoutSummary(orderItems, totalPrice, totalItems);
    }

    private void validateCheckoutFood(Food food, Restaurant restaurant) {
        if (food == null || food.getId() == null || food.getRestaurant() == null
                || food.getRestaurant().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart contains an invalid food item");
        }
        if (!restaurant.getId().equals(food.getRestaurant().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "All cart items must belong to the selected restaurant"
            );
        }
        if (!food.isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    food.getName() == null ? "A cart item is no longer available" : food.getName() + " is no longer available"
            );
        }
        if (food.getPrice() == null || food.getPrice() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A cart item has an invalid current price");
        }
    }

    private void validateCheckoutQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_CART_ITEM_QUANTITY) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cart item quantity must be between 1 and " + MAX_CART_ITEM_QUANTITY
            );
        }
    }

    private List<String> validateSelectedIngredients(
            List<String> selectedIngredients,
            Food food,
            Restaurant restaurant) {
        if (selectedIngredients == null || selectedIngredients.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, IngredientsItem> allowedIngredients = new HashMap<>();
        if (food.getIngredients() != null) {
            for (IngredientsItem ingredient : food.getIngredients()) {
                if (ingredient != null && ingredient.getName() != null) {
                    allowedIngredients.put(ingredient.getName().trim().toLowerCase(Locale.ROOT), ingredient);
                }
            }
        }

        List<String> validatedIngredients = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String selectedName : selectedIngredients) {
            String normalizedName = selectedName == null
                    ? ""
                    : selectedName.trim().toLowerCase(Locale.ROOT);
            IngredientsItem ingredient = allowedIngredients.get(normalizedName);
            boolean belongsToRestaurant = ingredient != null
                    && ingredient.getRestaurant() != null
                    && restaurant.getId().equals(ingredient.getRestaurant().getId());

            if (normalizedName.isEmpty() || !seen.add(normalizedName)
                    || !belongsToRestaurant || !ingredient.isInStock()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A selected ingredient is no longer available"
                );
            }
            validatedIngredients.add(ingredient.getName());
        }
        return validatedIngredients;
    }

    private record CheckoutSummary(List<OrderItem> items, Long totalPrice, int totalItems) {
    }

    @Override
    public Order updateOrder(Long orderId, String orderStatus, User actor) throws Exception {
        Order order = findOrderById(orderId);
        if (order.getRestaurant() == null) {
            throw new Exception("Order is not attached to a restaurant");
        }
        restaurantService.requireRestaurantManagementAccess(order.getRestaurant().getId(), actor);
        if (orderStatus == null || orderStatus.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order status is required");
        }

        String requestedStatus = orderStatus.trim().toUpperCase(Locale.ROOT);
        String currentStatus = order.getOrderStatus() == null
                ? "PENDING"
                : order.getOrderStatus().trim().toUpperCase(Locale.ROOT);

        if (requestedStatus.equals(currentStatus)) return order;
        if (order.getPaymentStatus() != PaymentStatus.PAID && !"CANCELLED".equals(requestedStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An unpaid order cannot enter fulfilment"
            );
        }

        Set<String> allowedStatuses = ORDER_TRANSITIONS.get(currentStatus);
        if (allowedStatuses == null || !allowedStatuses.contains(requestedStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order cannot move from " + currentStatus + " to " + requestedStatus
            );
        }

        order.setOrderStatus(requestedStatus);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order cancelOrder(Long orderId, User actor) throws Exception {
        if (actor == null || actor.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getCustomer() == null || !actor.getId().equals(order.getCustomer().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot cancel this order");
        }

        String currentStatus = order.getOrderStatus() == null
                ? "PENDING"
                : order.getOrderStatus().trim().toUpperCase(Locale.ROOT);
        if ("CANCELLED".equals(currentStatus)) return order;
        if (!"PENDING".equals(currentStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only pending orders can be cancelled"
            );
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Paid orders require restaurant support for cancellation"
            );
        }

        paymentService.cancelPendingPayment(order);
        order.setOrderStatus("CANCELLED");
        order.setPaymentStatus(PaymentStatus.PAYMENT_CANCELLED);
        return orderRepository.save(order);
    }

    @Override
    public List<Order> getUsersOrder(Long userId) throws Exception {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Order> getRestaurantsOrder(Long restaurantId, String orderStatus, User actor) throws Exception {
        restaurantService.requireRestaurantManagementAccess(restaurantId, actor);
        List<Order> orders = orderRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId).stream()
                .filter(order -> order.getPaymentStatus() == PaymentStatus.PAID)
                .collect(Collectors.toList());

        if(orderStatus != null && !orderStatus.isBlank() && !"ALL".equalsIgnoreCase(orderStatus)){
            orders = orders.stream()
                    .filter(order -> order.getOrderStatus() != null
                            && order.getOrderStatus().equalsIgnoreCase(orderStatus))
                    .collect(Collectors.toList());
        }

        return orders;
    }

    @Override
    public Order findOrderById(Long orderId) throws Exception {
        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        if(optionalOrder.isEmpty()){
            throw new Exception("Order not found!");
        }

        return optionalOrder.get();
    }

    @Override
    public Order findUsersOrderById(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid order is required");
        }
        return orderRepository.findByIdAndCustomerId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }
}
