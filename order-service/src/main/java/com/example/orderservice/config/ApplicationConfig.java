package com.example.orderservice.config;

import com.example.orderservice.application.*;
import com.example.orderservice.application.port.OrderEventPublisher;
import com.example.orderservice.application.port.OrderRepository;
import com.example.orderservice.application.port.ProcessedEventStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires application layer: use cases depend only on ports (interfaces).
 * Infrastructure implements the ports and is auto-wired by Spring.
 */
@Configuration
class ApplicationConfig {

    @Bean
    CreateOrderUseCase createOrderUseCase(OrderRepository orderRepository,
                                          OrderEventPublisher eventPublisher) {
        return new CreateOrderUseCase(orderRepository, eventPublisher);
    }

    @Bean
    HandleInventoryReservedUseCase handleInventoryReservedUseCase(OrderRepository orderRepository,
                                                                   OrderEventPublisher eventPublisher,
                                                                   ProcessedEventStore processedEventStore) {
        return new HandleInventoryReservedUseCase(orderRepository, eventPublisher, processedEventStore);
    }

    @Bean
    HandlePaymentFailedUseCase handlePaymentFailedUseCase(OrderRepository orderRepository,
                                                          ProcessedEventStore processedEventStore) {
        return new HandlePaymentFailedUseCase(orderRepository, processedEventStore);
    }

    @Bean
    HandleInventoryFailedUseCase handleInventoryFailedUseCase(OrderRepository orderRepository,
                                                              ProcessedEventStore processedEventStore) {
        return new HandleInventoryFailedUseCase(orderRepository, processedEventStore);
    }
}
