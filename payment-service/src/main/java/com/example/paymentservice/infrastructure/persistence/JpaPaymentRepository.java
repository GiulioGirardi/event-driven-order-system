package com.example.paymentservice.infrastructure.persistence;

import com.example.paymentservice.application.port.PaymentRepository;
import com.example.paymentservice.domain.Payment;
import com.example.paymentservice.domain.PaymentStatus;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class JpaPaymentRepository implements PaymentRepository {

    private final SpringDataPaymentRepository springData;

    JpaPaymentRepository(SpringDataPaymentRepository springData) {
        this.springData = springData;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = springData.findById(payment.getPaymentId())
                .map(e -> {
                    e.setStatus(payment.getStatus());
                    return e;
                })
                .orElse(new PaymentEntity(
                        payment.getPaymentId(),
                        payment.getOrderId(),
                        payment.getCustomerId(),
                        payment.getStatus(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getCreatedAt()
                ));
        springData.save(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<Payment> findById(UUID paymentId) {
        return springData.findById(paymentId).map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return springData.findByOrderId(orderId).map(this::toDomain);
    }

    private Payment toDomain(PaymentEntity e) {
        Payment payment = new Payment(
                e.getPaymentId(),
                e.getOrderId(),
                e.getCustomerId(),
                e.getAmount(),
                e.getCurrency(),
                e.getCreatedAt()
        );
        if (e.getStatus() == PaymentStatus.CONFIRMED) payment.confirm();
        else if (e.getStatus() == PaymentStatus.FAILED) payment.fail();
        else if (e.getStatus() == PaymentStatus.REFUNDED) {
            payment.confirm();
            payment.refund();
        }
        return payment;
    }
}
