package se.iths.armin.webshoporderservice.message;

import java.util.List;

/**
 * Innehåller beställningsuppgifterna som skickas via RabbitMQ till mail-service.
 * Mail-service använder uppgifterna för att skapa och skicka orderbekräftelsen.
 */
public class OrderConfirmationMessage {

    private String customerEmail;
    private String orderDate;
    private List<OrderItemSummary> items;
    private double totalPrice;

    /**
     * Produktnamn, beställt antal och styckpris för en rad i orderbekräftelsen.
     */
    public static class OrderItemSummary {
        private String name;
        private int quantity;
        private double price;

        public OrderItemSummary(String name, int quantity, double price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }

        public String getName() {
            return name;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getPrice() {
            return price;
        }
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public List<OrderItemSummary> getItems() {
        return items;
    }

    public void setItems(List<OrderItemSummary> items) {
        this.items = items;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}