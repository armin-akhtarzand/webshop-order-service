package se.iths.armin.webshoporderservice.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.iths.armin.webshoporderservice.entity.CustomerOrder;
import se.iths.armin.webshoporderservice.entity.PaymentStatus;
import se.iths.armin.webshoporderservice.repository.CustomerOrderRepository;
import se.iths.armin.webshoporderservice.service.CustomerOrderService;

import java.util.Optional;

@RestController
@RequestMapping("/stripe")
public class StripeWebhookController {

    private final CustomerOrderRepository customerOrderRepository;
    private final CustomerOrderService customerOrderService;


    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public StripeWebhookController(CustomerOrderRepository customerOrderRepository,
                                   CustomerOrderService customerOrderService) {
        this.customerOrderRepository = customerOrderRepository;
        this.customerOrderService = customerOrderService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            event = Webhook.constructEvent(
                    payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        String eventType = event.getType();

        if (!"checkout.session.completed".equals(eventType)
                && !"checkout.session.async_payment_succeeded".equals(eventType)
                && !"checkout.session.async_payment_failed".equals(eventType)
                && !"checkout.session.expired".equals(eventType)) {
            return ResponseEntity.ok("Event ignored");
        }

        Session session;

        try {
            session = (Session) event.getDataObjectDeserializer()
                    .deserializeUnsafe();
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Could not deserialize Stripe session");
        }

        if (session == null) {
            return ResponseEntity.badRequest().body("Missing Stripe session");
        }

        Optional<CustomerOrder> orderOptional =
                customerOrderRepository.findByStripeSessionId(session.getId());

        if (orderOptional.isEmpty()) {
            return ResponseEntity.ok("Order not found");
        }

        CustomerOrder order = orderOptional.get();

        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            return ResponseEntity.ok("Order already processed");
        }

        if ("checkout.session.completed".equals(eventType)
                || "checkout.session.async_payment_succeeded".equals(eventType)) {

            if (!"paid".equals(session.getPaymentStatus())) {
                return ResponseEntity.ok("Payment not completed");
            }

            order.setPaymentStatus(PaymentStatus.PAID);
            customerOrderRepository.save(order);

            //  Skicka orderbekräftelsen först efter att ordern markerats som betald.
            customerOrderService.sendOrderConfirmation(order);

        } else if ("checkout.session.async_payment_failed".equals(eventType)
                || "checkout.session.expired".equals(eventType)) {

            order.setPaymentStatus(PaymentStatus.FAILED);
            customerOrderRepository.save(order);
        }

        return ResponseEntity.ok("Received");
    }

}