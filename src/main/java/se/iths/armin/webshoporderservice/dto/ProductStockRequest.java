package se.iths.armin.webshoporderservice.dto;

public record ProductStockRequest(
        Long productId,
        int quantity
) {
}
