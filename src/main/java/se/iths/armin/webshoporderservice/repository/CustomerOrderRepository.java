package se.iths.armin.webshoporderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.iths.armin.webshoporderservice.entity.CustomerOrder;

public interface CustomerOrderRepository
        extends JpaRepository<CustomerOrder, Long> {
}
