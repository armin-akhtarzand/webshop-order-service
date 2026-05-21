package se.iths.armin.webshoporderservice.service;

import org.springframework.stereotype.Service;
import se.iths.armin.webshoporderservice.client.ProductClient;
import se.iths.armin.webshoporderservice.dto.CreateOrderItemRequest;
import se.iths.armin.webshoporderservice.dto.CreateOrderRequest;
import se.iths.armin.webshoporderservice.dto.ProductInfo;
import se.iths.armin.webshoporderservice.dto.ProductStockRequest;
import se.iths.armin.webshoporderservice.entity.CustomerOrder;
import se.iths.armin.webshoporderservice.entity.OrderItem;
import se.iths.armin.webshoporderservice.repository.CustomerOrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
public class CustomerOrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final ProductClient productClient;

    public CustomerOrderService(
            CustomerOrderRepository customerOrderRepository,
            ProductClient productClient) {

        this.customerOrderRepository = customerOrderRepository;
        this.productClient = productClient;
    }

    public List<CustomerOrder> findAll() {
        return customerOrderRepository.findAll();
    }

    public CustomerOrder createOrder(CreateOrderRequest request) {
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
        order.setCustomerName("test@example.com");
        order.setOrderItems(orderItems);
        order.setTotalPrice(totalPrice);

        return customerOrderRepository.save(order);
    }

}
