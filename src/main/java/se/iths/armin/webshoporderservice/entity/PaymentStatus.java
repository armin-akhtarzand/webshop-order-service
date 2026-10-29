package se.iths.armin.webshoporderservice.entity;

/**
 * Beskriver betalningsläget för en beställning.
 */
public enum PaymentStatus {
    /** Betalningen har ännu inte bekräftats. */
    PENDING,
    /** Stripe har bekräftat att betalningen är genomförd. */
    PAID,
    /** Betalningen har misslyckats eller checkout-sessionen har gått ut. */
    FAILED
}
