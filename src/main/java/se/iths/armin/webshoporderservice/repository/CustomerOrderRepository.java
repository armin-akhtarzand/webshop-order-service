package se.iths.armin.webshoporderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.iths.armin.webshoporderservice.entity.CustomerOrder;

import java.util.Optional;

/**
 * Hanterar lagring och hämtning av beställningar i databasen.
 */
public interface CustomerOrderRepository
        extends JpaRepository<CustomerOrder, Long> {

    /**
     * Söker efter beställningen som hör till en viss Stripe-session.
     *
     * @param stripeSessionId ID för Stripe-sessionen som sparats på beställningen
     * @return en Optional med beställningen om den finns, annars en tom Optional
     */
    Optional<CustomerOrder> findByStripeSessionId(String stripeSessionId);
}
