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

/**
 * Tar emot anrop för att skapa och hämta beställningar samt starta Stripe Checkout.
 */
@RestController
@RequestMapping("/orders")
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;

    public CustomerOrderController(CustomerOrderService customerOrderService) {
        this.customerOrderService = customerOrderService;
    }

    /**
     * Skapar beställning via CustomerOrderService
     *
     * @param request   produkter och antal som beställningen innehåller
     * @param principal autentiserade användaren gjort beställningen
     * @return den sparade beställningen
     */
    @PostMapping
    public CustomerOrder createOrder(@RequestBody CreateOrderRequest request,
                                     Principal principal) {
        String username = principal.getName();
        return customerOrderService.createOrder(request, username);
    }

    /**
     * Hämta beställningen via ID
     *
     * @param id beställningens ID
     * @return Ifall beställningen finns status 200 OK med ordern
     * annars status 404 utan body
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerOrder> getOrder(@PathVariable Long id) {
        return customerOrderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Skapar beställningen via CustomerOrderService
     * skapar en stripe session inför betalningen av beställningen via CustomerOrderService
     * sparar sessionens url och beställningen i en hashmap som response
     *
     * @param request   produkter och antal som beställningen gäller
     * @param principal vilken användare som gör beställningen
     * @return status 200 med betalningssidans url och beställningens id
     * @throws StripeException om anropet till stripe misslyckas
     */
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