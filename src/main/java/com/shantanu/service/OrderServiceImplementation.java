package com.shantanu.service;

import com.shantanu.model.*;
import com.shantanu.repository.AddressRepository;
import com.shantanu.repository.OrderItemRepository;
import com.shantanu.repository.OrderRepository;
import com.shantanu.repository.UserRepository;
import com.shantanu.request.AddressRequest;
import com.shantanu.request.OrderRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderServiceImplementation implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private CartService cartService;

    @Override
    @Transactional
    public Order createOrder(OrderRequest order, User user) throws Exception {
        if (order.getRestaurantId() == null) {
            throw new Exception("A valid restaurant is required");
        }

        Restaurant restaurant = restaurantService.findRestaurantById(order.getRestaurantId());

        Cart cart = cartService.findCartByUserId(user.getId());
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new Exception("Your cart is empty");
        }

        Address savedAddress = resolveDeliveryAddress(order.getDeliveryAddress(), user);

        Order createdOrder = new Order();
        createdOrder.setCustomer(user);
        createdOrder.setCreatedAt(new Date());
        createdOrder.setOrderStatus("PENDING");
        createdOrder.setDeliveryAddress(savedAddress);
        createdOrder.setRestaurant(restaurant);

        List<OrderItem> orderItems = new ArrayList<>();

        for(CartItem cartItem : cart.getItems()){
            OrderItem orderItem = new OrderItem();
            orderItem.setFood(cartItem.getFood());
            orderItem.setIngredients(cartItem.getIngredients());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setTotalPrice(cartItem.getTotalPrice());

            OrderItem savedOrderItem = orderItemRepository.save(orderItem);
            orderItems.add(savedOrderItem);
        }

        Long totalPrice = cartService.calculateCartTotals(cart);

        createdOrder.setItems(orderItems);
        createdOrder.setTotalPrice(totalPrice);

        Order savedOrder = orderRepository.save(createdOrder);
        restaurant.getOrders().add(savedOrder);

        return createdOrder;
    }

    private Address resolveDeliveryAddress(AddressRequest requestedAddress, User user) throws Exception {
        if (requestedAddress == null) {
            throw new Exception("Delivery address is required");
        }

        List<Address> savedAddresses = user.getAddresses();

        if (requestedAddress.getId() != null) {
            Address savedAddress = savedAddresses.stream()
                    .filter(address -> requestedAddress.getId().equals(address.getId()))
                    .findFirst()
                    .orElseThrow(() -> new Exception("Saved address was not found"));
            Address updatedAddress = toAddress(requestedAddress);
            validateAddress(updatedAddress);

            Optional<Address> duplicate = savedAddresses.stream()
                    .filter(address -> !address.getId().equals(savedAddress.getId()))
                    .filter(address -> addressesMatch(address, updatedAddress))
                    .findFirst();
            if (duplicate.isPresent()) {
                return duplicate.get();
            }

            if (!addressesMatch(savedAddress, updatedAddress)) {
                savedAddress.setFullName(updatedAddress.getFullName());
                savedAddress.setStreetAddress(updatedAddress.getStreetAddress());
                savedAddress.setCity(updatedAddress.getCity());
                savedAddress.setState(updatedAddress.getState());
                savedAddress.setPostalCode(updatedAddress.getPostalCode());
                savedAddress.setCountry(updatedAddress.getCountry());
                return addressRepository.save(savedAddress);
            }
            return savedAddress;
        }

        Address candidate = toAddress(requestedAddress);
        validateAddress(candidate);

        Optional<Address> duplicate = savedAddresses.stream()
                .filter(address -> addressesMatch(address, candidate))
                .findFirst();
        if (duplicate.isPresent()) {
            return duplicate.get();
        }

        Address savedAddress = addressRepository.save(candidate);
        savedAddresses.add(savedAddress);
        userRepository.save(user);
        return savedAddress;
    }

    private Address toAddress(AddressRequest requestedAddress) {
        Address address = new Address();
        address.setFullName(normalize(requestedAddress.getFullName()));
        address.setStreetAddress(normalize(requestedAddress.getStreetAddress()));
        address.setCity(normalize(requestedAddress.getCity()));
        address.setState(normalize(requestedAddress.getState()));
        address.setPostalCode(normalize(requestedAddress.getPostalCode()));
        address.setCountry(normalize(requestedAddress.getCountry()));
        return address;
    }

    private void validateAddress(Address address) throws Exception {
        if (isBlank(address.getFullName())
                || isBlank(address.getStreetAddress())
                || isBlank(address.getCity())
                || isBlank(address.getState())
                || isBlank(address.getPostalCode())
                || isBlank(address.getCountry())) {
            throw new Exception("Please provide a complete delivery address");
        }
    }

    private boolean addressesMatch(Address first, Address second) {
        Function<String, String> comparable = value -> normalize(value).toLowerCase(Locale.ROOT);
        return comparable.apply(first.getFullName()).equals(comparable.apply(second.getFullName()))
                && comparable.apply(first.getStreetAddress()).equals(comparable.apply(second.getStreetAddress()))
                && comparable.apply(first.getCity()).equals(comparable.apply(second.getCity()))
                && comparable.apply(first.getState()).equals(comparable.apply(second.getState()))
                && comparable.apply(first.getPostalCode()).equals(comparable.apply(second.getPostalCode()))
                && comparable.apply(first.getCountry()).equals(comparable.apply(second.getCountry()));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public Order updateOrder(Long orderId, String orderStatus) throws Exception {
        Order order = findOrderById(orderId);
        if(orderStatus.equals("OUT_FOR_DELIVERY") || orderStatus.equals("DELIVERED") || orderStatus.equals("COMPLETED") || orderStatus.equals("PENDING")){
            order.setOrderStatus(orderStatus);
            return orderRepository.save(order);
        }

        throw new Exception("Please select a valid order status");
    }

    @Override
    public void cancelOrder(Long orderId) throws Exception {
        Order order = findOrderById(orderId);
        orderRepository.deleteById(orderId);
    }

    @Override
    public List<Order> getUsersOrder(Long userId) throws Exception {
        return orderRepository.findByCustomerId(userId);
    }

    @Override
    public List<Order> getRestaurantsOrder(Long restaurantId, String orderStatus) throws Exception {
        List<Order> orders = orderRepository.findByRestaurantId(restaurantId);

        if(orderStatus != null){
            orders = orders.stream().filter(order -> order.getOrderStatus().equals(orderStatus)).collect(Collectors.toList());
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
}
