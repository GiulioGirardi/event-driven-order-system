# payment-service

Consumes **order.created** (from order-service), attempts charge via stub gateway, and publishes **payment.confirmed** or **payment.failed**.  
Consumes **payment.refund.requested** (from inventory-service when inventory fails after payment) and marks the payment REFUNDED (Saga compensation).

- **Port:** 8080 (mapped to 8081 in docker-compose when running with order-service)
- **DB:** PostgreSQL `paymentdb` (payments, processed_events)
- **Stub behaviour:** Set `app.payment.stub.fail-customer-id: fail-payment` to force payment failure for that customer; use `app.payment.stub.fail-above-amount` to fail when amount exceeds the value.
