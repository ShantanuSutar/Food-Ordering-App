package com.shantanu.controller;

import com.shantanu.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
public class PaymentWebhookController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader) throws Exception {
        try {
            paymentService.processWebhook(payload, signatureHeader);
            return ResponseEntity.ok("received");
        } catch (SignatureVerificationException error) {
            return ResponseEntity.badRequest().body("invalid signature");
        } catch (IllegalStateException error) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("webhook is not configured");
        }
    }
}
