# Topic Definitions

## Naming convention

- Format: `{domain}.{event-name}` (e.g. `order.created`).
- Dead Letter Topic: `{topic}.dlt` (e.g. `order.created.dlt`).

## Topic list

| Topic                      | Partitions | Key strategy        | Producer        | Consumers                          |
|----------------------------|------------|---------------------|-----------------|------------------------------------|
| order.created              | 6          | orderId             | order-service   | payment-service                    |
| order.created.dlt          | 1          | -                   | (Kafka/stream)  | -                                  |
| payment.confirmed          | 6          | orderId             | payment-service | inventory-service                  |
| payment.confirmed.dlt      | 1          | -                   | -               | -                                  |
| payment.failed             | 6          | orderId             | payment-service | order-service                      |
| payment.failed.dlt         | 1          | -                   | -               | -                                  |
| payment.refund.requested   | 6          | orderId             | inventory-svc   | payment-service                    |
| payment.refund.requested.dlt | 1        | -                   | -               | -                                  |
| inventory.reserved         | 6          | orderId             | inventory-svc   | order-service                      |
| inventory.reserved.dlt     | 1          | -                   | -               | -                                  |
| inventory.failed           | 6          | orderId             | inventory-svc   | order-service, payment-service*    |
| inventory.failed.dlt       | 1          | -                   | -               | -                                  |
| order.confirmed            | 6          | orderId             | order-service   | notification-service               |
| order.confirmed.dlt        | 1          | -                   | -               | -                                  |

\* payment-service consumes inventory.failed to decide whether to refund (when it had previously confirmed payment for that order). Alternatively, compensation is triggered by inventory-service publishing payment.refund.requested so payment-service only needs to subscribe to payment.refund.requested. We use the latter to keep a single responsibility: inventory-service publishes refund request; payment-service only reacts to it.

## Consumer groups

- `order-service-order-created`: N/A (order-service does not consume order.created).
- `payment-service-order-created`: consumes order.created.
- `inventory-service-payment-confirmed`: consumes payment.confirmed.
- `order-service-payment-failed`: consumes payment.failed.
- `order-service-inventory-reserved`: consumes inventory.reserved.
- `order-service-inventory-failed`: consumes inventory.failed.
- `payment-service-payment-refund-requested`: consumes payment.refund.requested.
- `notification-service-order-confirmed`: consumes order.confirmed.

## Key strategy

Partition by **orderId** so all events for the same order are in the same partition. This preserves ordering per order and helps idempotency and saga consistency.
