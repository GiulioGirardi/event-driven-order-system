package com.example.paymentservice.application;

import com.example.paymentservice.application.port.PaymentRepository;
import com.example.paymentservice.application.port.ProcessedEventStore;
import com.example.paymentservice.domain.Payment;
import com.example.paymentservice.domain.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Handles PaymentRefundRequested (Saga compensation): find payment by paymentId, perform refund, mark REFUNDED.
 * Idempotent by eventId; duplicate refund requests are no-op.
 */
public class ProcessRefundRequestedUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessRefundRequestedUseCase.class);

    private final PaymentRepository paymentRepository;
    private final ProcessedEventStore processedEventStore;

    public ProcessRefundRequestedUseCase(PaymentRepository paymentRepository,
                                         ProcessedEventStore processedEventStore) {
        this.paymentRepository = paymentRepository;
        this.processedEventStore = processedEventStore;
    }

    /**
     * @return true if refund was applied; false if duplicate event or payment not found / not CONFIRMED
     */
    public boolean execute(String eventId, String paymentId, String orderId) {
        if (processedEventStore.alreadyProcessed(eventId)) {
            log.debug("Duplicate PaymentRefundRequested event ignored, eventId={}", eventId);
            return false;
        }
        Payment payment = paymentRepository.findById(UUID.fromString(paymentId)).orElse(null);
        if (payment == null) {
            log.warn("Payment not found for refund, paymentId={} orderId={}", paymentId, orderId);
            processedEventStore.markProcessed(eventId);
            return false;
        }
        if (payment.getStatus() != PaymentStatus.CONFIRMED) {
            log.debug("Payment not CONFIRMED, cannot refund, paymentId={} status={}", paymentId, payment.getStatus());
            processedEventStore.markProcessed(eventId);
            return false;
        }
        payment.refund();
        paymentRepository.save(payment);
        processedEventStore.markProcessed(eventId);
        log.info("Payment refunded paymentId={} orderId={} (Saga compensation)", paymentId, orderId);
        return true;
    }
}
