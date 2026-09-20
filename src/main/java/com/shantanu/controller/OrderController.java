package com.shantanu.controller;

import com.shantanu.model.Order;
import com.shantanu.model.User;
import com.shantanu.request.OrderRequest;
import com.shantanu.request.PaymentVerificationRequest;
import com.shantanu.response.PaymentResponse;
import com.shantanu.response.PaymentVerificationResponse;
import com.shantanu.service.OrderService;
import com.shantanu.service.PaymentService;
import com.shantanu.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<PaymentResponse> createOrder(@RequestBody OrderRequest req, @RequestHeader("Authorization") String jwt) throws Exception{
        User user = userService.findUserByJwtToken(jwt);
        Order order = orderService.createOrder(req, user);
        PaymentResponse res = paymentService.createPaymentLink(order);
        res.setDeliveryAddress(order.getDeliveryAddress());
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }


    @GetMapping("/order/user")
    public ResponseEntity<List<Order>> getOrderHistory(@RequestHeader("Authorization") String jwt) throws Exception{
        User user = userService.findUserByJwtToken(jwt);
        List<Order> orders = orderService.getUsersOrder(user.getId());
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @GetMapping("/order/{id}")
    public ResponseEntity<Order> getOrder(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserByJwtToken(jwt);
        return ResponseEntity.ok(orderService.findUsersOrderById(id, user.getId()));
    }

    @PutMapping("/order/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserByJwtToken(jwt);
        return ResponseEntity.ok(orderService.cancelOrder(id, user));
    }

    @PostMapping("/payment/verify")
    public ResponseEntity<PaymentVerificationResponse> verifyPayment(
            @RequestBody PaymentVerificationRequest request,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserByJwtToken(jwt);
        PaymentVerificationResponse response = paymentService.verifyAndFinalizePayment(
                request.getSessionId(),
                request.getOrderId(),
                user.getId()
        );
        HttpStatus status = response.isVerified() ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return new ResponseEntity<>(response, status);
    }
}
