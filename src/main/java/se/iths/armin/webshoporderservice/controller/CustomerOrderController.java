package se.iths.armin.webshoporderservice.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.iths.armin.webshoporderservice.dto.CreateOrderRequest;
import se.iths.armin.webshoporderservice.entity.CustomerOrder;
import se.iths.armin.webshoporderservice.service.CustomerOrderService;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

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

    @GetMapping("/{id}")
    public ResponseEntity<CustomerOrder> getOrder(@PathVariable Long id) {
        return customerOrderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> createCheckout(
            @RequestBody CreateOrderRequest request,
            Principal principal) throws StripeException {

        CustomerOrder order =
                customerOrderService.createOrder(request, principal.getName());

        Session session =
                customerOrderService.createCheckoutSession(order);

        Map<String, Object> response = new HashMap<>();
        response.put("checkoutUrl", session.getUrl());
        response.put("orderId", order.getId());

        return ResponseEntity.ok(response);
    }
}   