package se.iths.armin.webshoporderservice.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_CONFIRMATION_QUEUE =
            "order-confirmation-queue";

    @Bean
    public Queue orderConfirmationQueue() {
        return new Queue(ORDER_CONFIRMATION_QUEUE);
    }
}