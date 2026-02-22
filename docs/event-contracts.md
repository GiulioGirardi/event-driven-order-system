# Event Contracts

All events use a **common envelope** for idempotency, tracing, and versioning. Domain payload is nested under `payload`.

## Envelope (all events)

```json
{
  "eventId": "uuid",
  "eventType": "string",
  "version": "string",
  "correlationId": "string",
  "timestamp": "ISO-8601",
  "payload": { ... }
}
```

- **eventId**: Unique per event; used as idempotency key by consumers.
- **eventType**: Discriminator (e.g. `OrderCreated`).
- **version**: Schema version (e.g. `1.0`) for evolution.
- **correlationId**: Set at order creation; propagated through the saga for logging/tracing.
- **timestamp**: Event occurrence time (UTC).

---

## 1. OrderCreated (order-service → )

**Topic:** `order.created`

**Payload:**
```json
{
  "orderId": "uuid",
  "customerId": "string",
  "totalAmount": "number",
  "currency": "string",
  "items": [
    {
      "productId": "string",
      "quantity": "integer",
      "unitPrice": "number"
    }
  ]
}
```

**Consumers:** payment-service.

---

## 2. PaymentConfirmed (payment-service → )

**Topic:** `payment.confirmed`

**Payload:**
```json
{
  "paymentId": "uuid",
  "orderId": "uuid",
  "amount": "number",
  "currency": "string",
  "status": "CONFIRMED"
}
```

**Consumers:** inventory-service.

---

## 3. PaymentFailed (payment-service → )

**Topic:** `payment.failed`

**Payload:**
```json
{
  "paymentId": "uuid",
  "orderId": "uuid",
  "amount": "number",
  "currency": "string",
  "status": "FAILED",
  "reason": "string"
}
```

**Consumers:** order-service (update order to FAILED).

---

## 4. InventoryReserved (inventory-service → )

**Topic:** `inventory.reserved`

**Payload:**
```json
{
  "reservationId": "uuid",
  "orderId": "uuid",
  "items": [
    {
      "productId": "string",
      "quantity": "integer",
      "reservedQuantity": "integer"
    }
  ]
}
```

**Consumers:** order-service (confirm order, then publish OrderConfirmed).

---

## 5. InventoryFailed (inventory-service → )

**Topic:** `inventory.failed`

**Payload:**
```json
{
  "orderId": "uuid",
  "reason": "string",
  "items": [
    {
      "productId": "string",
      "requestedQuantity": "integer",
      "availableQuantity": "integer"
    }
  ]
}
```

**Consumers:** order-service (mark order failed), payment-service (trigger refund via PaymentRefundRequested or internal flow).  
Compensation: payment-service must refund; we use `payment.refund.requested` for that.

---

## 6. PaymentRefundRequested (compensation)

**Topic:** `payment.refund.requested`

**Payload:**
```json
{
  "refundId": "uuid",
  "paymentId": "uuid",
  "orderId": "uuid",
  "amount": "number",
  "currency": "string",
  "reason": "string"
}
```

**Published by:** inventory-service when it fails after payment was already confirmed (Saga compensation).  
**Consumers:** payment-service (perform refund).

---

## 7. OrderConfirmed (order-service → )

**Topic:** `order.confirmed`

**Payload:**
```json
{
  "orderId": "uuid",
  "customerId": "string",
  "status": "CONFIRMED",
  "confirmedAt": "ISO-8601"
}
```

**Consumers:** notification-service (send notification).
