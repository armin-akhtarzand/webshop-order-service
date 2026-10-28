package se.iths.armin.webshoporderservice.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Ställer in HTTP-klienten som order-service använder för anrop till product-service.
 */
@Configuration
public class ClientConfig {

    /**
     * Skapar en klient med adressen till product-service från konfigurationen.
     *
     * @param baseUrl grundadressen till product-service
     * @return klienten som används för produktanrop
     */
    @Bean
    public RestClient productRestClient(
            @Value("${product-service.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
