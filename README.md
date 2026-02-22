# Event-Driven Order System

Distributed order processing system with four microservices communicating via Kafka. Each service has its own PostgreSQL database. No direct REST calls between services.

## Quick start

From the repo root:

```bash
docker compose up --build
```

This builds order-service inside Docker (no local Gradle required) and starts Kafka, Zookeeper, PostgreSQL, and order-service. To run order-service locally instead, set `POSTGRES_HOST`, `KAFKA_BOOTSTRAP_SERVERS`, etc., and run `gradle bootJar` (or `./gradlew bootJar` if you generate the wrapper) then `java -jar build/libs/order-service-*.jar` from the `order-service` directory (requires Gradle 8.x and JDK 17).

This starts Zookeeper, Kafka, PostgreSQL (order + payment DBs), **order-service** (http://localhost:8080), and **payment-service** (http://localhost:8081).

Create an order:

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"cust-1","totalAmount":99.99,"currency":"USD","items":[{"productId":"P1","quantity":2,"unitPrice":49.99}]}'
```

Optional: create Kafka topics explicitly before starting services:

```bash
docker compose --profile init up -d kafka
docker compose --profile init run --rm kafka-init
docker compose up order-service
```

## Project layout

- **order-service** — Creates orders (PENDING), publishes `OrderCreated`, consumes `InventoryReserved` / `PaymentFailed` / `InventoryFailed`, publishes `OrderConfirmed`.
- **payment-service** — Consumes `OrderCreated`, charges (stub), publishes `PaymentConfirmed` or `PaymentFailed`; consumes `PaymentRefundRequested` (Saga compensation) and marks payment REFUNDED.
- **inventory-service**, **notification-service** — Placeholders (to be implemented).
- **docs/** — [architecture.md](docs/architecture.md), [event-contracts.md](docs/event-contracts.md), [topics.md](docs/topics.md).
- **docker-compose.yml** — Kafka, Zookeeper, one PostgreSQL per service, order-service.

## Architecture

See [docs/architecture.md](docs/architecture.md) for system overview, saga, idempotency, failure handling, and trade-offs.

## Event flow (happy path)

1. Client → order-service: create order → status PENDING, publish **OrderCreated**.
2. payment-service consumes OrderCreated → charges → **PaymentConfirmed** or **PaymentFailed**.
3. inventory-service consumes PaymentConfirmed → reserves → **InventoryReserved** or **InventoryFailed**.
4. order-service consumes InventoryReserved → status CONFIRMED → publish **OrderConfirmed**.
5. notification-service consumes OrderConfirmed → send notification.

Compensation: if inventory fails after payment, inventory-service publishes **PaymentRefundRequested**; payment-service refunds.
"# event-driven-order-system" 
