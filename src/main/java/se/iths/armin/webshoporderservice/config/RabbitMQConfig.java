package se.iths.armin.webshoporderservice.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ställer in kön och JSON-formatet för orderbekräftelser som skickas till mail-service.
 */
@Configuration
public class RabbitMQConfig {

    /** Namnet på kön som order-service och mail-service använder för orderbekräftelser. */
    public static final String ORDER_CONFIRMATION_QUEUE =
            "order-confirmation-queue";

    /**
     * Definierar kön där orderbekräftelser lämnas för mejlutskick.
     * Kön är beständig, vilket betyder att själva kön finns kvar vid omstart av RabbitMQ.
     *
     * @return kön för orderbekräftelser
     */
    @Bean
    public Queue orderConfirmationQueue() {
        return new Queue(ORDER_CONFIRMATION_QUEUE);
    }

    /**
     * Skapar verktyget som omvandlar meddelandeobjekt till JSON inför publicering.
     *
     * @return JSON-omvandlaren för RabbitMQ-meddelanden
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * Skapar verktyget som publicerar meddelanden till RabbitMQ i JSON-format.
     *
     * @param connectionFactory anslutningen som används för att nå RabbitMQ
     * @param messageConverter verktyget som omvandlar meddelandet till JSON
     * @return verktyget som order-service använder för att publicera meddelanden
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}