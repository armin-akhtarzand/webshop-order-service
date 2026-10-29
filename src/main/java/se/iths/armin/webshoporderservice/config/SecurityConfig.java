package se.iths.armin.webshoporderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Ställer in JWT-verifiering och vilka anrop som kräver inloggning i order-service.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Kräver en giltig JWT för orderanrop och sparar ingen inloggningssession på servern.
     * Swagger och Stripe-webhooken tillåts utan JWT.
     * Webhookens signatur kontrolleras istället i StripeWebhookController.
     * CSRF-skyddet är avstängt; tjänsten tar emot JWT via Authorization-headern.
     *
     * @param http inställningar för HTTP-säkerhet
     * @return säkerhetsreglerna för inkommande anrop
     * @throws Exception om säkerhetsreglerna inte kan skapas
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/stripe/webhook"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                );

        return http.build();
    }

    /**
     * Bestämmer vilka lokala frontend-adresser som får göra anrop från webbläsaren,
     * samt vilka HTTP-metoder och headers som tillåts.
     *
     * @return CORS-inställningarna för tjänstens endpoints
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:5175"
        ));
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type"
        ));

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}