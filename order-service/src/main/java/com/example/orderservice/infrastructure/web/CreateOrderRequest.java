package com.example.orderservice.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank String customerId,
        @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
        @NotBlank String currency,
        @NotEmpty @Valid List<OrderItemRequest> items
) {
    public record OrderItemRequest(
            @NotBlank String productId,
            @NotNull Integer quantity,
            @NotNull @DecimalMin("0") BigDecimal unitPrice
    ) {}
}
