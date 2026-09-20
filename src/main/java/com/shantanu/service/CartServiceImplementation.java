package com.shantanu.service;

import com.shantanu.model.Cart;
import com.shantanu.model.CartItem;
import com.shantanu.model.Food;
import com.shantanu.model.User;
import com.shantanu.repository.CartItemRepository;
import com.shantanu.repository.CartRepository;
import com.shantanu.request.AddCartItemRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class CartServiceImplementation implements CartService{

    private static final int MAX_CART_ITEM_QUANTITY = 99;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private FoodService foodService;

    @Override
    public CartItem addItemToCart(AddCartItemRequest req, String jwt) throws Exception {
        if (req == null || req.getFoodId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid food item is required");
        }
        validateQuantity(req.getQuantity());

        User user = userService.findUserByJwtToken(jwt);
        Food food = foodService.findFoodById(req.getFoodId());
        Cart cart = cartRepository.findByCustomerId(user.getId());
        if (cart == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found");
        }

        for(CartItem cartItem : cart.getItems()){
            if(cartItem.getFood() != null && cartItem.getFood().getId().equals(food.getId())){
                int newQuantity = cartItem.getQuantity() + req.getQuantity();
                return updateOwnedCartItemQuantity(cartItem, newQuantity);
            }
        }

        CartItem newCartItem = new CartItem();
        newCartItem.setFood(food);
        newCartItem.setCart(cart);
        newCartItem.setQuantity(req.getQuantity());
        newCartItem.setIngredients(req.getIngredients());
        newCartItem.setTotalPrice(req.getQuantity() * food.getPrice());

        CartItem savedCartItem = cartItemRepository.save(newCartItem);

        cart.getItems().add(savedCartItem);
        cart.setTotal(calculateCartTotals(cart));
        cartRepository.save(cart);

        return savedCartItem;
    }

    @Override
    public CartItem updateCartItemQuantity(Long cartItemId, int quantity, String jwt) throws Exception {
        validateQuantity(quantity);
        User user = userService.findUserByJwtToken(jwt);
        CartItem item = findOwnedCartItem(cartItemId, user.getId());
        return updateOwnedCartItemQuantity(item, quantity);
    }

    private CartItem updateOwnedCartItemQuantity(CartItem item, int quantity) {
        validateQuantity(quantity);
        item.setQuantity(quantity);
        item.setTotalPrice(item.getFood().getPrice() * quantity);

        CartItem savedItem = cartItemRepository.save(item);
        Cart cart = item.getCart();
        if (cart != null) {
            cart.setTotal(calculateCartTotals(cart));
            cartRepository.save(cart);
        }
        return savedItem;
    }

    @Override
    public Cart removeItemFromCart(Long cartItemId, String jwt) throws Exception {
        User user = userService.findUserByJwtToken(jwt);
        Cart cart = cartRepository.findByCustomerId(user.getId());
        if (cart == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found");
        }

        CartItem item = findOwnedCartItem(cartItemId, user.getId());
        cart.getItems().removeIf(cartItem -> cartItem.getId().equals(item.getId()));
        cart.setTotal(calculateCartTotals(cart));

        return cartRepository.save(cart);
    }

    @Override
    public Long calculateCartTotals(Cart cart) {
        Long total = 0L;

        for(CartItem cartItem : cart.getItems()){
            total += cartItem.getFood().getPrice() * cartItem.getQuantity();
        }

        return total;
    }

    @Override
    public Cart findCartById(Long id) throws Exception {
        Optional<Cart> optionalCart = cartRepository.findById(id);
        if(optionalCart.isEmpty()){
            throw new Exception("Cart not found with id " + id);
        }
        return optionalCart.get();
    }

    @Override
    public Cart findCartByUserId(Long userId) throws Exception {
        Cart cart = cartRepository.findByCustomerId(userId);
        if (cart == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found");
        }
        cart.setTotal(calculateCartTotals(cart));
        return cart;
    }

    @Override
    public Cart clearCart(Long userId) throws Exception {
        Cart cart = findCartByUserId(userId);
        cart.getItems().clear();
        cart.setTotal(0L);
        return cartRepository.save(cart);
    }

    private CartItem findOwnedCartItem(Long cartItemId, Long userId) {
        if (cartItemId == null || userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid cart item is required");
        }
        return cartItemRepository.findByIdAndCart_Customer_Id(cartItemId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
    }

    private void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart item quantity must be at least 1");
        }
        if (quantity > MAX_CART_ITEM_QUANTITY) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cart item quantity cannot exceed " + MAX_CART_ITEM_QUANTITY
            );
        }
    }
}
