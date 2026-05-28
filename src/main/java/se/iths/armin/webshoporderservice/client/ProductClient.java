package se.iths.armin.webshoporderservice.client;


import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String token = jwt.getTokenValue();

        return restClient.post()
                .uri("/products/stock/decrease")
                .header("Authorization", "Bearer " + token)
                .body(items)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
