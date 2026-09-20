package com.shantanu.service;

import com.shantanu.model.Order;
import com.shantanu.model.PaymentStatus;
import com.shantanu.repository.OrderRepository;
import com.shantanu.response.PaymentResponse;
import com.shantanu.response.PaymentHistoryResponse;
import com.shantanu.response.PaymentVerificationResponse;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.ApiResource;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.Collections;
import java.util.List;

@Service
public class PaymentServiceImplementation implements PaymentService {

    private static final String DEFAULT_CHECKOUT_CURRENCY = "inr";

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.payment.currency:inr}")
    private String checkoutCurrency;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartService cartService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse createPaymentLink(Order order) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        String currency = normalizedCheckoutCurrency();
        long amount = checkoutAmount(order);

        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(order.getId().toString())
                .setSuccessUrl(frontendUrl + "/payment/success/" + order.getId()
                        + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/payment/fail?order_id=" + order.getId())
                .putMetadata("order_id", order.getId().toString())
                .putMetadata("user_id", order.getCustomer().getId().toString())
                .setBillingAddressCollection(SessionCreateParams.BillingAddressCollection.REQUIRED)
                .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                        .setDescription("DineHub order #" + order.getId())
                        .putMetadata("order_id", order.getId().toString())
                        .putMetadata("user_id", order.getCustomer().getId().toString())
                        .build())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currency)
                                .setUnitAmount(amount)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("DineHub order #" + order.getId())
                                        .build())
                                .build())
                        .build());

        String customerEmail = order.getCustomer().getEmail();
        if (customerEmail != null && !customerEmail.isBlank()) {
            paramsBuilder.setCustomerEmail(customerEmail.trim());
        }

        Session session = Session.create(paramsBuilder.build());
        if (session.getUrl() == null || session.getUrl().isBlank()) {
            throw new IllegalStateException("Stripe did not return a checkout URL");
        }
        order.setStripeSessionId(session.getId());
        order.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        order.setPaymentCurrency(currency);
        orderRepository.save(order);

        PaymentResponse response = new PaymentResponse();
        response.setPayment_url(session.getUrl());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVerificationResponse verifyAndFinalizePayment(
            String sessionId,
            Long orderId,
            Long userId) throws Exception {
        Order order = findOrderForUpdate(orderId);
        verifyOrderOwnership(order, userId);
        verifyStoredSession(order, sessionId);

        if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new IllegalStateException("This order has been cancelled");
        }

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
    @Transactional(rollbackFor = Exception.class)
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

        if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
            return;
        }

        if ("checkout.session.completed".equals(eventType)
                || "checkout.session.async_payment_succeeded".equals(eventType)) {
            if ("paid".equalsIgnoreCase(session.getPaymentStatus())) {
                finalizeSuccessfulPayment(order, session);
            }
            return;
        }

        if (order.getPaymentStatus() != PaymentStatus.PAID
                && order.getPaymentStatus() != PaymentStatus.PAYMENT_CANCELLED) {
            order.setPaymentStatus(PaymentStatus.PAYMENT_FAILED);
            orderRepository.save(order);
        }
    }

    @Override
    public void cancelPendingPayment(Order order) throws Exception {
        if (order.getPaymentStatus() == PaymentStatus.PAYMENT_FAILED
                || order.getPaymentStatus() == PaymentStatus.PAYMENT_CANCELLED) {
            return;
        }
        if (order.getStripeSessionId() == null || order.getStripeSessionId().isBlank()) {
            return;
        }

        Stripe.apiKey = stripeSecretKey;
        Session session = Session.retrieve(order.getStripeSessionId());
        if ("paid".equalsIgnoreCase(session.getPaymentStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment has already completed for this order"
            );
        }
        String expireUrl = Stripe.getApiBase()
                + "/v1/checkout/sessions/"
                + ApiResource.urlEncodeId(order.getStripeSessionId())
                + "/expire";
        ApiResource.request(
                ApiResource.RequestMethod.POST,
                expireUrl,
                Collections.emptyMap(),
                Session.class,
                (RequestOptions) null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> getPaymentHistory(Long userId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(userId).stream()
                .filter(order -> order.getPaymentStatus() != null)
                .map(this::toPaymentHistoryResponse)
                .toList();
    }

    private void finalizeSuccessfulPayment(Order order, Session session) throws Exception {
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return;
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStripePaymentIntentId(session.getPaymentIntent());
        order.setPaidAmount(session.getAmountTotal());
        order.setPaymentCurrency(session.getCurrency());
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
        String metadataUserId = session.getMetadata() == null
                ? null
                : session.getMetadata().get("user_id");
        Long expectedAmount = Math.multiplyExact(order.getTotalPrice(), 100L);
        String expectedCurrency = order.getPaymentCurrency();
        if (expectedCurrency == null || expectedCurrency.isBlank()) {
            expectedCurrency = normalizedCheckoutCurrency();
        }

        if (!order.getId().equals(sessionOrderId)
                || clientReferenceId == null
                || !order.getId().toString().equals(clientReferenceId)
                || metadataUserId == null
                || !order.getCustomer().getId().toString().equals(metadataUserId)
                || !expectedAmount.equals(session.getAmountTotal())
                || session.getCurrency() == null
                || !expectedCurrency.equalsIgnoreCase(session.getCurrency())) {
            throw new Exception("Stripe session does not match this order");
        }
    }

    private Long extractOrderId(Session session) throws Exception {
        String metadataOrderId = session.getMetadata() == null
                ? null
                : session.getMetadata().get("order_id");
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

    private PaymentHistoryResponse toPaymentHistoryResponse(Order order) {
        Long amount = order.getPaidAmount();
        if (amount == null && order.getTotalPrice() != null) {
            amount = Math.multiplyExact(order.getTotalPrice(), 100L);
        }

        String currency = order.getPaymentCurrency();
        if (currency == null || currency.isBlank()) {
            currency = normalizedCheckoutCurrency();
        }

        return new PaymentHistoryResponse(
                order.getId(),
                order.getRestaurant() == null ? null : order.getRestaurant().getId(),
                order.getRestaurant() == null ? null : order.getRestaurant().getName(),
                order.getCreatedAt(),
                order.getPaidAt(),
                amount,
                currency,
                order.getPaymentStatus(),
                order.getStripePaymentIntentId(),
                "Stripe Checkout"
        );
    }

    private String normalizedCheckoutCurrency() {
        String currency = checkoutCurrency == null ? "" : checkoutCurrency.trim().toLowerCase();
        if (!currency.matches("[a-z]{3}")) {
            return DEFAULT_CHECKOUT_CURRENCY;
        }
        return currency;
    }

    private long checkoutAmount(Order order) {
        if (order == null || order.getId() == null || order.getCustomer() == null
                || order.getCustomer().getId() == null || order.getTotalPrice() == null
                || order.getTotalPrice() <= 0) {
            throw new IllegalArgumentException("Order is not ready for payment");
        }
        try {
            return Math.multiplyExact(order.getTotalPrice(), 100L);
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("Order total is too large for payment", error);
        }
    }
}
