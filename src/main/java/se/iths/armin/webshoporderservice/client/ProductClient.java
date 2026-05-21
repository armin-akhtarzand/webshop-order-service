package se.iths.armin.webshoporderservice.client;


import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import se.iths.armin.webshoporderservice.dto.ProductInfo;
import se.iths.armin.webshoporderservice.dto.ProductStockRequest;

import java.util.List;

@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<ProductInfo> decreaseStock(List<ProductStockRequest> items) {
        return restClient.post()
                .uri("/products/stock/decrease")
                .body(items)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
