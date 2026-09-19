package com.shantanu.service;

import com.shantanu.model.Order;
import com.shantanu.model.PaymentStatus;
import com.shantanu.repository.OrderRepository;
import com.shantanu.response.PaymentResponse;
import com.shantanu.response.PaymentVerificationResponse;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class PaymentServiceImplementation implements PaymentService {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartService cartService;

    @Override
    @Transactional
    public PaymentResponse createPaymentLink(Order order) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(order.getId().toString())
                .setSuccessUrl(frontendUrl + "/payment/success/" + order.getId()
                        + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/payment/fail?order_id=" + order.getId())
                .putMetadata("order_id", order.getId().toString())
                .putMetadata("user_id", order.getCustomer().getId().toString())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount(order.getTotalPrice() * 100)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("DineHub order #" + order.getId())
                                        .build())
                                .build())
                        .build())
                .build();

        Session session = Session.create(params);
        order.setStripeSessionId(session.getId());
        order.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        orderRepository.save(order);

        PaymentResponse response = new PaymentResponse();
        response.setPayment_url(session.getUrl());
        return response;
    }

    @Override
    @Transactional
    public PaymentVerificationResponse verifyAndFinalizePayment(
            String sessionId,
            Long orderId,
            Long userId) throws Exception {
        Order order = findOrderForUpdate(orderId);
        verifyOrderOwnership(order, userId);
        verifyStoredSession(order, sessionId);

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return verifiedResponse(order);
        }

        Stripe.apiKey = stripeSecretKey;
        Session session = Session.retrieve(sessionId);
        validateSessionOrder(session, order);

        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            return new PaymentVerificationResponse(
                    false,
                    "Payment is still pending",
                    order.getPaymentStatus().name()
            );
        }

        finalizeSuccessfulPayment(order, session);
        return verifiedResponse(order);
    }

    @Override
    @Transactional
    public void processWebhook(String payload, String signatureHeader) throws Exception {
        if (stripeWebhookSecret == null || stripeWebhookSecret.isBlank()) {
            throw new IllegalStateException("Stripe webhook secret is not configured");
        }

        Event event = Webhook.constructEvent(payload, signatureHeader, stripeWebhookSecret);
        String eventType = event.getType();

        if (!"checkout.session.completed".equals(eventType)
                && !"checkout.session.async_payment_succeeded".equals(eventType)
                && !"checkout.session.async_payment_failed".equals(eventType)
                && !"checkout.session.expired".equals(eventType)) {
            return;
        }

        Session session = deserializeSession(event);
        Long orderId = extractOrderId(session);
        Order order = findOrderForUpdate(orderId);
        verifyStoredSession(order, session.getId());
        validateSessionOrder(session, order);

        if ("checkout.session.completed".equals(eventType)
                || "checkout.session.async_payment_succeeded".equals(eventType)) {
            if ("paid".equalsIgnoreCase(session.getPaymentStatus())) {
                finalizeSuccessfulPayment(order, session);
            }
            return;
        }

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.PAYMENT_FAILED);
            orderRepository.save(order);
        }
    }

    private void finalizeSuccessfulPayment(Order order, Session session) throws Exception {
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return;
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStripePaymentIntentId(session.getPaymentIntent());
        order.setPaidAt(new Date());
        orderRepository.save(order);
        cartService.clearCart(order.getCustomer().getId());
    }

    private Order findOrderForUpdate(Long orderId) throws Exception {
        if (orderId == null) {
            throw new Exception("Order id is required");
        }
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new Exception("Order not found"));
    }

    private void verifyOrderOwnership(Order order, Long userId) throws Exception {
        if (userId == null || !order.getCustomer().getId().equals(userId)) {
            throw new Exception("Payment does not belong to this account");
        }
    }

    private void verifyStoredSession(Order order, String sessionId) throws Exception {
        if (sessionId == null || sessionId.isBlank()
                || order.getStripeSessionId() == null
                || !order.getStripeSessionId().equals(sessionId)) {
            throw new Exception("Payment session does not match this order");
        }
    }

    private void validateSessionOrder(Session session, Order order) throws Exception {
        Long sessionOrderId = extractOrderId(session);
        String clientReferenceId = session.getClientReferenceId();
        String metadataUserId = session.getMetadata().get("user_id");
        Long expectedAmount = Math.multiplyExact(order.getTotalPrice(), 100L);

        if (!order.getId().equals(sessionOrderId)
                || clientReferenceId == null
                || !order.getId().toString().equals(clientReferenceId)
                || metadataUserId == null
                || !order.getCustomer().getId().toString().equals(metadataUserId)
                || !expectedAmount.equals(session.getAmountTotal())
                || !"usd".equalsIgnoreCase(session.getCurrency())) {
            throw new Exception("Stripe session does not match this order");
        }
    }

    private Long extractOrderId(Session session) throws Exception {
        String metadataOrderId = session.getMetadata().get("order_id");
        if (metadataOrderId == null || metadataOrderId.isBlank()) {
            throw new Exception("Stripe session is missing order metadata");
        }
        try {
            return Long.valueOf(metadataOrderId);
        } catch (NumberFormatException error) {
            throw new Exception("Stripe session has invalid order metadata");
        }
    }

    private Session deserializeSession(Event event) throws Exception {
        StripeObject stripeObject = event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new Exception("Could not deserialize Stripe checkout session"));
        if (!(stripeObject instanceof Session)) {
            throw new Exception("Stripe event does not contain a checkout session");
        }
        return (Session) stripeObject;
    }

    private PaymentVerificationResponse verifiedResponse(Order order) {
        return new PaymentVerificationResponse(
                true,
                "Payment verified",
                order.getPaymentStatus().name()
        );
    }
}
