package com.shantanu.controller;

import com.shantanu.model.CartItem;
import com.shantanu.model.Order;
import com.shantanu.model.User;
import com.shantanu.request.AddCartItemRequest;
import com.shantanu.request.OrderRequest;
import com.shantanu.response.PaymentResponse;
import com.shantanu.response.PaymentVerificationResponse;
import com.shantanu.service.OrderService;
import com.shantanu.service.PaymentService;
import com.shantanu.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserService userService;

    @PostMapping("/order")
    public ResponseEntity<PaymentResponse> createOrder(@RequestBody OrderRequest req, @RequestHeader("Authorization") String jwt) throws Exception{
        User user = userService.findUserByJwtToken(jwt);
        Order order = orderService.createOrder(req, user);
        PaymentResponse res = paymentService.createPaymentLink(order);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }


    @GetMapping("/order/user")
    public ResponseEntity<List<Order>> getOrderHistory(@RequestHeader("Authorization") String jwt) throws Exception{
        User user = userService.findUserByJwtToken(jwt);
        List<Order> orders = orderService.getUsersOrder(user.getId());
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @GetMapping("/payment/verify")
    public ResponseEntity<PaymentVerificationResponse> verifyPayment(
            @RequestParam("session_id") String sessionId,
            @RequestParam("order_id") Long orderId,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserByJwtToken(jwt);
        Order order = orderService.findOrderById(orderId);

        if (!order.getCustomer().getId().equals(user.getId())) {
            return new ResponseEntity<>(
                    new PaymentVerificationResponse(false, "Payment does not belong to this account"),
                    HttpStatus.FORBIDDEN
            );
        }

        boolean verified = paymentService.verifyPayment(sessionId, orderId);
        HttpStatus status = verified ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        String message = verified ? "Payment verified" : "Payment could not be verified";

        return new ResponseEntity<>(new PaymentVerificationResponse(verified, message), status);
    }
}
