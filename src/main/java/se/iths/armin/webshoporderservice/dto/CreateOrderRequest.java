package se.iths.armin.webshoporderservice.dto;

import java.util.List;

public class CreateOrderRequest {
    private List<CreateOrderItemRequest> items;
    
    public List<CreateOrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CreateOrderItemRequest> items) {
        this.items = items;
    }
}
