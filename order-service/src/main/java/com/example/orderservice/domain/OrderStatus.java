package com.example.orderservice.domain;

/**
 * Order lifecycle status. Transitions are driven by events from payment and inventory.
 */
public enum OrderStatus {
    PENDING,   // Created; payment not yet confirmed
    CONFIRMED, // Payment and inventory succeeded
    FAILED     // Payment failed or inventory failed (or compensation path)
}
