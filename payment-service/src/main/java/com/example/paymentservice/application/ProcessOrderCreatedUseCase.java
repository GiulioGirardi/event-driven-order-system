package com.example.paymentservice.application;

import com.example.paymentservice.application.port.ChargeGateway;
import com.example.paymentservice.application.port.PaymentEventPublisher;
import com.example.paymentservice.application.port.PaymentRepository;
import com.example.paymentservice.application.port.ProcessedEventStore;
import com.example.paymentservice.domain.Payment;
import com.example.paymentservice.domain.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Handles OrderCreated: create payment (PENDING), attempt charge, publish PaymentConfirmed or PaymentFailed.
 * Idempotent by eventId; duplicate OrderCreated for same order is ignored after first processing.
 */
public class ProcessOrderCreatedUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessOrderCreatedUseCase.class);

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;
    private final ProcessedEventStore processedEventStore;
    private final ChargeGateway chargeGateway;

    public ProcessOrderCreatedUseCase(PaymentRepository paymentRepository,
                                      PaymentEventPublisher eventPublisher,
                                      ProcessedEventStore processedEventStore,
                                      ChargeGateway chargeGateway) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.processedEventStore = processedEventStore;
        this.chargeGateway = chargeGateway;
    }

    /**
     * @return true if event was processed (charge attempted and event published); false if duplicate
     */
    public boolean execute(String eventId, String correlationId, String orderId, String customerId,
                           BigDecimal totalAmount, String currency) {
        if (processedEventStore.alreadyProcessed(eventId)) {
            log.debug("Duplicate OrderCreated event ignored, eventId={}", eventId);
            return false;
        }
        UUID orderUuid = UUID.fromString(orderId);
        // Already have a payment for this order? (e.g. duplicate event or another replica)
        if (paymentRepository.findByOrderId(orderUuid).isPresent()) {
            log.debug("Payment already exists for orderId={}, skipping duplicate", orderId);
            processedEventStore.markProcessed(eventId);
            return false;
        }

        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment(paymentId, orderUuid, customerId, totalAmount, currency, Instant.now());
        paymentRepository.save(payment);

        ChargeGateway.ChargeResult result = chargeGateway.charge(orderUuid, totalAmount, currency, customerId);

        if (result.success()) {
            payment.confirm();
            paymentRepository.save(payment);
            String confirmedEventId = UUID.randomUUID().toString();
            eventPublisher.publishPaymentConfirmed(new PaymentEventPublisher.PaymentConfirmedEvent(
                    confirmedEventId,
                    correlationId,
                    paymentId.toString(),
                    orderId,
                    totalAmount,
                    currency,
                    PaymentStatus.CONFIRMED.name()
            ));
            log.info("Payment confirmed orderId={} paymentId={} correlationId={}", orderId, paymentId, correlationId);
        } else {
            payment.fail();
            paymentRepository.save(payment);
            String failedEventId = UUID.randomUUID().toString();
            eventPublisher.publishPaymentFailed(new PaymentEventPublisher.PaymentFailedEvent(
                    failedEventId,
                    correlationId,
                    paymentId.toString(),
                    orderId,
                    totalAmount,
                    currency,
                    PaymentStatus.FAILED.name(),
                    result.failureReason()
            ));
            log.warn("Payment failed orderId={} reason={} correlationId={}", orderId, result.failureReason(), correlationId);
        }
        processedEventStore.markProcessed(eventId);
        return true;
    }
}
