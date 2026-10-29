package se.iths.armin.webshoporderservice.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.iths.armin.webshoporderservice.client.ProductClient;
import se.iths.armin.webshoporderservice.config.RabbitMQConfig;
import se.iths.armin.webshoporderservice.dto.CreateOrderItemRequest;
import se.iths.armin.webshoporderservice.dto.CreateOrderRequest;
import se.iths.armin.webshoporderservice.dto.ProductInfo;
import se.iths.armin.webshoporderservice.dto.ProductStockRequest;
import se.iths.armin.webshoporderservice.entity.CustomerOrder;
import se.iths.armin.webshoporderservice.entity.OrderItem;
import se.iths.armin.webshoporderservice.message.OrderConfirmationMessage;
import se.iths.armin.webshoporderservice.repository.CustomerOrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Hanterar orderskapning, lageranrop, Stripe Checkout och publicering av orderbekräftelser.
 */
@Service
public class CustomerOrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final ProductClient productClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    public CustomerOrderService(
            CustomerOrderRepository customerOrderRepository,
            ProductClient productClient,
            RabbitTemplate rabbitTemplate) {

        this.customerOrderRepository = customerOrderRepository;
        this.productClient = productClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Hämtar alla beställningar.
     *
     * @return en lista med alla beställningar, eller en tom lista om inga finns
     */
    public List<CustomerOrder> findAll() {
        return customerOrderRepository.findAll();
    }

    /**
     * Skapar beställningen med angivna produkter och antal
     * Minskar lagersaldot via product-service
     * räknar ut totalpriset med hjälp av priserna från product-service
     * sparar beställningen med betalningsstatus pending (default)
     *
     * @param request  produkterna och antal som beställningen innehåller
     * @param username mejlen på användaren som gör beställningen
     * @return den sparade beställningen
     */
    public CustomerOrder createOrder(CreateOrderRequest request, String username) {
        List<ProductStockRequest> stockRequests = new ArrayList<>();

        for (CreateOrderItemRequest item : request.getItems()) {
            stockRequests.add(new ProductStockRequest(item.getProductId(), item.getQuantity()));
        }

        List<ProductInfo> products = productClient.decreaseStock(stockRequests);

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (ProductInfo product : products) {
            CreateOrderItemRequest requestedItem = request.getItems().stream()
                    .filter(item -> item.getProductId().equals(product.id()))
                    .findFirst()
                    .orElseThrow();

            OrderItem orderItem = new OrderItem();
            orderItem.setName(product.name());
            orderItem.setPrice(product.price());
            orderItem.setQuantity(requestedItem.getQuantity());

            orderItems.add(orderItem);

            totalPrice = totalPrice.add(
                    product.price().multiply(BigDecimal.valueOf(requestedItem.getQuantity()))
            );
        }

        CustomerOrder order = new CustomerOrder();
        order.setOrderDate(LocalDateTime.now());
        order.setCustomerName(username);
        order.setOrderItems(orderItems);
        order.setTotalPrice(totalPrice);

        CustomerOrder savedOrder =
                customerOrderRepository.save(order);

        return savedOrder;
    }

    /**
     * Bygger en orderbekräftelse med beställarens mejl,
     * beställningsdatum, produkter, antal och totalpris
     * Publicerar meddelandet till RabbitMQ för mejlutskick via mail-service
     *
     * @param savedOrder den sparade beställningen som bekräftelsen gäller
     */
    public void sendOrderConfirmation(CustomerOrder savedOrder) {
        OrderConfirmationMessage message = new OrderConfirmationMessage();

        message.setCustomerEmail(savedOrder.getCustomerName());
        message.setOrderDate(savedOrder.getOrderDate().toString());

        List<OrderConfirmationMessage.OrderItemSummary> items =
                new ArrayList<>();

        for (OrderItem item : savedOrder.getOrderItems()) {
            items.add(new OrderConfirmationMessage.OrderItemSummary(
                    item.getName(),
                    item.getQuantity(),
                    item.getPrice().doubleValue()
            ));
        }

        message.setItems(items);
        message.setTotalPrice(savedOrder.getTotalPrice().doubleValue());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_CONFIRMATION_QUEUE,
                message
        );
    }


    /**
     * Hämtar en beställning via ID.
     *
     * @param id ID för beställningen som ska hämtas
     * @return en Optional med beställningen om den finns, annars en tom Optional
     */
    public Optional<CustomerOrder> getOrderById(Long id) {
        return customerOrderRepository.findById(id);
    }


    /**
     * Skapar en Stripe Checkout-session för beställningen.
     * Anger valuta, belopp i ören och adresser för slutförd eller avbruten checkout.
     * Sparar Stripe-sessionens ID på beställningen.
     *
     * @param order beställningen som ska betalas
     * @return den skapade Stripe-sessionen med bland annat betalningssidans URL
     * @throws StripeException om anropet till Stripe misslyckas
     */
    public Session createCheckoutSession(CustomerOrder order) throws StripeException {

        Stripe.apiKey = stripeSecretKey;

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl("http://localhost:5173/payment/success?orderId=" + order.getId())
                        .setCancelUrl("http://localhost:5173/payment/cancel")
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("sek")
                                                        .setUnitAmount(
                                                                order.getTotalPrice()
                                                                        .multiply(new BigDecimal("100"))
                                                                        .longValue()
                                                        )
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .setName("Webshop order #" + order.getId())
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .build();

        Session session = Session.create(params);

        order.setStripeSessionId(session.getId());
        customerOrderRepository.save(order);

        return session;
    }


}
