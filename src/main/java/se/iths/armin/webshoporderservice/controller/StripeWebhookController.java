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

import java.util.Optional;

@RestController
@RequestMapping("/stripe")
public class StripeWebhookController {

    private final CustomerOrderRepository customerOrderRepository;


    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public StripeWebhookController(CustomerOrderRepository customerOrderRepository) {
        this.customerOrderRepository = customerOrderRepository;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            event = Webhook.constructEvent(
                    payload,
                    sigHeader,
                    webhookSecret
            );
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        Session session;

        if ("checkout.session.completed".equals(event.getType())) {

            try {
                session = (Session) event.getDataObjectDeserializer()
                        .deserializeUnsafe();
            } catch (Exception e) {
                System.out.println("Could not deserialize Stripe session");
                e.printStackTrace();
                return ResponseEntity.ok("Session could not be deserialized");
            }

            String stripeSessionId = session.getId();

            System.out.println("Stripe session ID: " + stripeSessionId);

            Optional<CustomerOrder> orderOptional =
                    customerOrderRepository.findByStripeSessionId(stripeSessionId);

            if (orderOptional.isEmpty()) {
                System.out.println("No order found for Stripe session ID: " + stripeSessionId);
                return ResponseEntity.ok("Order not found");
            }

            CustomerOrder order = orderOptional.get();

            System.out.println("Order found: " + order.getId());

            if (order.getPaymentStatus() == PaymentStatus.PENDING) {
                order.setPaymentStatus(PaymentStatus.PAID);
                customerOrderRepository.save(order);

                System.out.println("Order marked as PAID: " + order.getId());
            }
        }

        return ResponseEntity.ok("Received");
    }
}