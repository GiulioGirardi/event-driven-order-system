# Event-Driven Order System — Architecture

## 1. System Overview

Distributed order processing system where four services collaborate **only via Kafka**. No service-to-service HTTP. Each service owns its database and applies clean architecture (domain, application, infrastructure).

**Principles:** event-driven flow, eventual consistency, idempotent consumers, basic Saga with compensation (e.g. refund on inventory failure).

---

## 2. Service Boundaries

| Service              | Responsibility                          | DB content (conceptual)     | Publishes                          | Consumes                                      |
|----------------------|------------------------------------------|-----------------------------|------------------------------------|-----------------------------------------------|
| **order-service**    | Order lifecycle, orchestration view      | Orders, status              | OrderCreated, OrderConfirmed       | InventoryReserved, PaymentFailed, InventoryFailed |
| **payment-service**  | Charge and refund                       | Payments                    | PaymentConfirmed, PaymentFailed    | OrderCreated, PaymentRefundRequested          |
| **inventory-service**| Reserve / release stock                 | Stock, reservations         | InventoryReserved, InventoryFailed, PaymentRefundRequested | PaymentConfirmed |
| **notification-service** | Notifications (e.g. email/log)      | Notification log            | -                                  | OrderConfirmed                                |

Boundaries are **aggregate-oriented**: Order, Payment, Inventory, Notification. Each service is the single writer of its aggregate and reacts to events from others.

---

## 3. Topic Definitions

See [event-contracts.md](event-contracts.md) and [topics.md](topics.md) for full detail.

**Main topics:** `order.created`, `order.confirmed`, `payment.confirmed`, `payment.failed`, `payment.refund.requested`, `inventory.reserved`, `inventory.failed`.

**Keys:** All use `orderId` (or equivalent) as Kafka key so per-order ordering is preserved in one partition.

**DLT:** Every main topic has a corresponding `.<topic>.dlt` for messages that exhaust retries.

---

## 4. Event Schemas

- **Envelope:** `eventId`, `eventType`, `version`, `correlationId`, `timestamp`, `payload`.
- **eventId:** Idempotency key; consumers store processed eventIds and skip duplicates.
- **correlationId:** Set at first API call (order creation), propagated in all events for logging and tracing.
- **version:** Schema version (e.g. `1.0`) to support evolution and compatibility checks.

Payloads are JSON; see [event-contracts.md](event-contracts.md) for each event type.

---

## 5. Saga Explanation

We use a **choreography-style saga**: no central orchestrator; each service reacts to events and may emit follow-up or compensation events.

**Happy path:**

1. Client → order-service: create order → status PENDING.
2. order-service → `order.created`.
3. payment-service consumes → charges → `payment.confirmed` or `payment.failed`.
4. inventory-service consumes `payment.confirmed` → reserves → `inventory.reserved` or `inventory.failed`.
5. order-service consumes `inventory.reserved` → status CONFIRMED → `order.confirmed`.
6. notification-service consumes `order.confirmed` → logs/sends notification.

**Compensation (inventory fails after payment):**

- inventory-service publishes `inventory.failed` (and optionally `payment.refund.requested` with paymentId/orderId/amount).
- payment-service consumes `payment.refund.requested` → refunds.
- order-service consumes `payment.failed` or `inventory.failed` → sets order to FAILED.

**Payment fails:** order-service consumes `payment.failed` → order FAILED; no inventory or refund needed.

**Idempotency** at each step ensures duplicate events (e.g. after consumer restart or retries) do not double-charge, double-reserve, or double-confirm.

---

## 6. Idempotency Strategy

- **Producer:** Each outbound event has a unique `eventId` (e.g. UUID). Optionally, producer can deduplicate by (orderId + eventType) when recreating the same logical event (e.g. after retry).
- **Consumer:** Before applying side effects (DB write, publish), check if `eventId` was already processed (e.g. `processed_events` table or column on aggregate). If seen → skip (and ack). If not → process, store eventId, then ack.

Processed eventIds should be retained for at least the topic retention (e.g. 7 days) or longer if compliance requires.

---

## 7. Failure Strategy

- **Retries:** Consumer retries with backoff (e.g. exponential). After N failures, send to DLT (e.g. via `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`).
- **DLT:** Messages that exhaust retries go to `.<topic>.dlt`. No automatic replay; ops can inspect and optionally republish or fix and reprocess.
- **Payment failure:** order-service marks order FAILED on `payment.failed`; no compensation.
- **Inventory failure:** order-service marks order FAILED on `inventory.failed`; if payment was already confirmed, inventory-service (or a dedicated step) publishes `payment.refund.requested` so payment-service can refund (Saga compensation).
- **Consumer restart:** Offsets committed after successful processing; idempotency by eventId prevents duplicate application of the same event.

---

## 8. Trade-offs

- **Choreography vs orchestration:** Choreography keeps services decoupled and avoids a single point of failure; downside is flow visibility (we rely on events + correlationId + docs).
- **Ordering:** Partitioning by orderId gives per-order ordering; global ordering across orders is not guaranteed (and not required).
- **Exactly-once:** We aim for “effectively once” via idempotency; Kafka exactly-once (EOS) is an option for stricter guarantees at the cost of complexity and performance.
- **DLT handling:** Manual or semi-automated; no built-in auto-replay to avoid accidental duplicate processing.

---

## 9. Future Improvements

- Schema registry (Avro/JSON Schema) for contracts and compatibility checks.
- Outbox pattern in each service to publish events in the same transaction as domain writes.
- Metrics and tracing (e.g. Micrometer, OpenTelemetry) with correlationId.
- Explicit order state machine (e.g. PENDING → PAYMENT_PENDING → RESERVING → CONFIRMED / FAILED) and events for each transition if needed for UX or ops.
- Idempotency window/cleanup (e.g. TTL or periodic purge of old processed eventIds).
- Health checks that verify Kafka connectivity and consumer lag.
