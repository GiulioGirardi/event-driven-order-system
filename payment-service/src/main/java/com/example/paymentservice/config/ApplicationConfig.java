package com.example.paymentservice.config;

import com.example.paymentservice.application.ProcessOrderCreatedUseCase;
import com.example.paymentservice.application.ProcessRefundRequestedUseCase;
import com.example.paymentservice.application.port.ChargeGateway;
import com.example.paymentservice.application.port.PaymentEventPublisher;
import com.example.paymentservice.application.port.PaymentRepository;
import com.example.paymentservice.application.port.ProcessedEventStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ApplicationConfig {

    @Bean
    ProcessOrderCreatedUseCase processOrderCreatedUseCase(PaymentRepository paymentRepository,
                                                         PaymentEventPublisher eventPublisher,
                                                         ProcessedEventStore processedEventStore,
                                                         ChargeGateway chargeGateway) {
        return new ProcessOrderCreatedUseCase(paymentRepository, eventPublisher, processedEventStore, chargeGateway);
    }

    @Bean
    ProcessRefundRequestedUseCase processRefundRequestedUseCase(PaymentRepository paymentRepository,
                                                                ProcessedEventStore processedEventStore) {
        return new ProcessRefundRequestedUseCase(paymentRepository, processedEventStore);
    }
}
