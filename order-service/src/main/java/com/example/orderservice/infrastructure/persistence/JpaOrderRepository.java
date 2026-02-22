package com.example.orderservice.infrastructure.persistence;

import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderStatus;
import com.example.orderservice.application.port.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
class JpaOrderRepository implements OrderRepository {

    private final SpringDataOrderRepository springData;

    JpaOrderRepository(SpringDataOrderRepository springData) {
        this.springData = springData;
    }

    @Override
    public Order save(Order order) {
        List<OrderEntity.OrderItemEmbeddable> items = order.getItems().stream()
                .map(i -> new OrderEntity.OrderItemEmbeddable(i.productId(), i.quantity(), i.unitPrice()))
                .collect(Collectors.toList());
        OrderEntity entity = springData.findById(order.getOrderId())
                .map(e -> {
                    e.setStatus(order.getStatus());
                    return e;
                })
                .orElse(new OrderEntity(
                        order.getOrderId(),
                        order.getCustomerId(),
                        order.getStatus(),
                        order.getTotalAmount(),
                        order.getCurrency(),
                        items,
                        order.getCreatedAt()
                ));
        springData.save(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<Order> findById(UUID orderId) {
        return springData.findById(orderId).map(this::toDomain);
    }

    private Order toDomain(OrderEntity e) {
        var items = e.getItems().stream()
                .map(i -> new Order.OrderItem(i.productId, i.quantity, i.unitPrice))
                .toList();
        Order order = new Order(
                e.getOrderId(),
                e.getCustomerId(),
                e.getTotalAmount(),
                e.getCurrency(),
                items,
                e.getCreatedAt()
        );
        if (e.getStatus() == OrderStatus.CONFIRMED) order.confirm();
        if (e.getStatus() == OrderStatus.FAILED) order.fail();
        return order;
    }
}
