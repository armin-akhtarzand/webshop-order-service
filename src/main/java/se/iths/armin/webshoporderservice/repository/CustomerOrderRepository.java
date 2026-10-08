package se.iths.armin.webshoporderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.iths.armin.webshoporderservice.entity.CustomerOrder;

import java.util.Optional;

public interface CustomerOrderRepository
        extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByStripeSessionId(String stripeSessionId);
}
