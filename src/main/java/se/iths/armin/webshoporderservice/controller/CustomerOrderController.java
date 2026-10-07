package se.iths.armin.webshoporderservice.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.iths.armin.webshoporderservice.dto.CreateOrderRequest;
import se.iths.armin.webshoporderservice.entity.CustomerOrder;
import se.iths.armin.webshoporderservice.service.CustomerOrderService;

import java.security.Principal;

@RestController
@RequestMapping("/orders")
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;

    public CustomerOrderController(CustomerOrderService customerOrderService) {
        this.customerOrderService = customerOrderService;
    }

    @PostMapping
    public CustomerOrder createOrder(@RequestBody CreateOrderRequest request,
                                     Principal principal) {
        String username = principal.getName();
        return customerOrderService.createOrder(request, username);
    }

    @PostMapping("/checkout")
    public ResponseEntity<String> createCheckout(
            @RequestBody CreateOrderRequest request,
            Principal principal) throws StripeException {

        CustomerOrder order =
                customerOrderService.createOrder(request, principal.getName());

        Session session =
                customerOrderService.createCheckoutSession(order);

        return ResponseEntity.ok(session.getUrl());
    }
}   