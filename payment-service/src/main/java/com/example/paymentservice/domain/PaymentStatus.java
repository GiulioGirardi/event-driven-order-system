package com.example.paymentservice.domain;

/**
 * Payment lifecycle: charge attempt then either CONFIRMED, FAILED, or later REFUNDED (Saga compensation).
 */
public enum PaymentStatus {
    PENDING,   // Charge in progress
    CONFIRMED, // Charge succeeded
    FAILED,    // Charge failed
    REFUNDED   // Charge succeeded but later refunded (e.g. inventory failed)
}
