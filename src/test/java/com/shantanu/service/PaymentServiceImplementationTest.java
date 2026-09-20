package com.shantanu.service;

import com.shantanu.model.Order;
import com.shantanu.model.PaymentStatus;
import com.shantanu.model.Restaurant;
import com.shantanu.model.User;
import com.shantanu.repository.OrderRepository;
import com.shantanu.response.PaymentHistoryResponse;
import com.shantanu.response.PaymentVerificationResponse;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplementationTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartService cartService;

    @InjectMocks
    private PaymentServiceImplementation paymentService;

    private Order order;

    @BeforeEach
    void setUp() {
        User customer = new User();
        customer.setId(5L);

        order = new Order();
        order.setId(12L);
        order.setCustomer(customer);
        order.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        order.setStripeSessionId("cs_paid");
        order.setTotalPrice(250L);

    }

    @Test
    void paidStripeSessionClearsPersistentCartExactlyOnce() throws Exception {
        when(orderRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(order));
        Session session = mock(Session.class);
        when(session.getMetadata()).thenReturn(Map.of("order_id", "12", "user_id", "5"));
        when(session.getClientReferenceId()).thenReturn("12");
        when(session.getAmountTotal()).thenReturn(25000L);
        when(session.getCurrency()).thenReturn("inr");
        when(session.getPaymentStatus()).thenReturn("paid");
        when(session.getPaymentIntent()).thenReturn("pi_123");

        try (MockedStatic<Session> stripeSessions = mockStatic(Session.class)) {
            stripeSessions.when(() -> Session.retrieve("cs_paid")).thenReturn(session);

            PaymentVerificationResponse first = paymentService.verifyAndFinalizePayment("cs_paid", 12L, 5L);
            PaymentVerificationResponse duplicate = paymentService.verifyAndFinalizePayment("cs_paid", 12L, 5L);

            assertTrue(first.isVerified());
            assertTrue(duplicate.isVerified());
            assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
            assertEquals("pi_123", order.getStripePaymentIntentId());
            assertEquals(25000L, order.getPaidAmount());
            assertEquals("inr", order.getPaymentCurrency());
            verify(orderRepository, times(1)).save(order);
            verify(cartService, times(1)).clearCart(5L);
        }
    }

    @Test
    void mismatchedSessionNeverClearsCart() throws Exception {
        when(orderRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(order));
        assertThrows(
                Exception.class,
                () -> paymentService.verifyAndFinalizePayment("cs_other", 12L, 5L)
        );

        verify(orderRepository, never()).save(order);
        verify(cartService, never()).clearCart(5L);
    }

    @Test
    void paymentHistoryUsesSafeStoredPaymentDetails() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(3L);
        restaurant.setName("Real Restaurant");
        order.setRestaurant(restaurant);
        order.setCreatedAt(new Date());
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaidAmount(25000L);
        order.setPaymentCurrency("usd");
        order.setStripePaymentIntentId("pi_123");
        when(orderRepository.findByCustomerIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(order));

        List<PaymentHistoryResponse> history = paymentService.getPaymentHistory(5L);

        assertEquals(1, history.size());
        PaymentHistoryResponse payment = history.get(0);
        assertEquals(12L, payment.getOrderId());
        assertEquals("Real Restaurant", payment.getRestaurantName());
        assertEquals(25000L, payment.getAmount());
        assertEquals("usd", payment.getCurrency());
        assertEquals("pi_123", payment.getPaymentIntentId());
        assertEquals("Stripe Checkout", payment.getPaymentMethod());
    }
}
