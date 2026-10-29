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

/**
 * Anropar product-service för att minska lagret och hämta produktinformationen.
 * Skickar med den inloggade användarens JWT från det aktuella anropet.
 */
@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Hämtar den inloggad användarens JWT via SecurityContext
     * vi anropar product-service endpoint stock/decrease som i sin tur ska "sänka" lagersaldot utifrån de produkter vi begär.
     * Anropet skickar även med token för att visa att det är autentiserad användare som gör request
     * vi skickar med vilka produkter det gäller
     *
     * @param items lista på produkter samt antal som lagersaldot ska dras ifrån
     *              * @return produktinformation med ID, namn, pris och beställt antal från product-service
     */
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
