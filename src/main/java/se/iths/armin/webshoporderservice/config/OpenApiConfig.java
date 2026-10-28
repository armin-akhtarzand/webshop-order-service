package se.iths.armin.webshoporderservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ställer in API-dokumentationen som visas i Swagger.
 */
@Configuration
public class OpenApiConfig {


    /**
     * Anger API-information och hur en Bearer-token skickas vid testning i Swagger.
     * Denna inställning beskriver API:t men skyddar inte anropen i sig.
     *
     * @return API-dokumentationens inställningar
     */
    @Bean
    public OpenAPI openAPI() {
        String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .description("Authentication and authorization service for the webshop microservice architecture. Handles JWT authentication, user management and role-based access control.")
                        .version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components()
                        .addSecuritySchemes(schemeName, new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
