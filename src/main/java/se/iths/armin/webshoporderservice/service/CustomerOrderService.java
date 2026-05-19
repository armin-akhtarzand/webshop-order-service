package se.iths.armin.webshoporderservice.service;

import org.springframework.stereotype.Service;
import se.iths.armin.webshoporderservice.entity.CustomerOrder;
import se.iths.armin.webshoporderservice.repository.CustomerOrderRepository;

import java.util.List;


@Service
public class CustomerOrderService {

    private final CustomerOrderRepository customerOrderRepository;

    public CustomerOrderService(CustomerOrderRepository customerOrderRepository) {
        this.customerOrderRepository = customerOrderRepository;
    }

    public List<CustomerOrder> findAll() {
        return customerOrderRepository.findAll();
    }
}
