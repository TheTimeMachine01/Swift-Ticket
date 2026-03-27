# Technical Design Document (TDD): SwiftTick

This document provides the exact technical specifications, data models, and contracts required to build the SwiftTick application. It is designed to act as a strict blueprint for code generation and implementation.

## 0. Base Package Structure (Spring Modulith Requirement)
Spring Modulith strictly relies on top-level packages to define boundaries.
*   **Base Package:** `com.swifttick`
*   **Modules:**
    *   `com.swifttick.event`
    *   `com.swifttick.booking`
    *   `com.swifttick.payment`
    *   `com.swifttick.notification`
    *   `com.swifttick.common` (for shared cross-cutting concerns like security filters)

## 1. Data Models & Database Schema (MySQL)

**1.1 Enums**
*   `SeatStatus`: `AVAILABLE`, `LOCKED`, `BOOKED`
*   `OrderStatus`: `PENDING`, `CONFIRMED`, `FAILED`, `EXPIRED`

**1.2 Entities**

*   **`Event` (Event Module)**
    *   `id` (Long, PK, Auto-increment)
    *   `name` (String)
    *   `date` (Timestamp)
    *   `total_seats` (Integer)

*   **`Seat` (Event Module)**
    *   `id` (Long, PK, Auto-increment)
    *   `event_id` (Long, FK to Event)
    *   `seat_number` (String)
    *   `price` (BigDecimal) - **(Required for order calculation)**
    *   `status` (Enum: SeatStatus)
    *   `version` (Long, `@Version` for Optimistic Locking)

*   **`Order` (Booking Module)**
    *   `id` (UUID, PK)
    *   `user_id` (Long, Indexed)
    *   `seat_id` (Long, FK to Seat)
    *   `amount` (BigDecimal)
    *   `status` (Enum: OrderStatus)
    *   `created_at` (Timestamp)
    *   `expires_at` (Timestamp) - **(To handle cart abandonment)**

*   **`IdempotencyKey` (Booking Module)**
    *   Stored in **Redis** with a 24-hour TTL.
    *   `Key`: `idempotency:{key_val}`
    *   `Value`: Serialized HTTP Response.

## 2. API Contracts

**2.1 Authentication Context**
*   For MVP, authentication is mocked via an `X-User-ID` (Long) HTTP header required on all secured endpoints.
*   The Token Bucket Rate Limiter will key off this `X-User-ID` (or IP address if header is missing).

**2.2 Endpoints**

*   **GET `/api/v1/events/{eventId}/seats`**
    *   **Response:** `200 OK` with JSON array of `{ seatId, seatNumber, price, status }`.
    *   *Note:* Should be cached in Redis.

*   **POST `/api/v1/bookings`**
    *   **Headers:** `X-User-ID: <Long>`, `Idempotency-Key: <String>`
    *   **Request Body:** `{ "eventId": Long, "seatId": Long }`
    *   **Response (Success):** `201 Created` - `{ "orderId": UUID, "status": "PENDING", "expiresAt": "ISO-8601" }`
    *   **Response (Locked/Booked):** `409 Conflict` - `{ "error": "Seat is temporarily locked or already booked." }`
    *   **Response (Idempotent Hit):** `200 OK` - Returns previously cached response from Redis.

*   **POST `/api/v1/payments`**
    *   **Headers:** `X-User-ID: <Long>`, `Idempotency-Key: <String>`
    *   **Request Body:** `{ "orderId": UUID, "paymentMethod": "CREDIT_CARD" }`
    *   **Response:** `200 OK` - `{ "orderId": UUID, "status": "CONFIRMED" }`

## 3. Concurrency, Locking & Cart Abandonment Flow

**3.1 The Booking Flow (Preventing the Thundering Herd)**
1.  **Idempotency Check:** Verify `Idempotency-Key` in Redis. If exists, return cached response.
2.  **Fast-Fail Check (Redis):** Check Redis key `seat:status:{seatId}`. If it equals `LOCKED` or `BOOKED`, immediately throw an exception (409 Conflict) without hitting MySQL.
3.  **Distributed Lock (Redis):** Attempt to acquire a Redisson lock using key `ticket:lock:{eventId}:{seatId}`.
    *   Wait time: 2 seconds. Lease time: 10 seconds.
    *   If acquisition fails, throw `SeatTemporarilyLockedException` (returns `409 Conflict`).
4.  **Database Check & Update:**
    *   Fetch `Seat` from MySQL. Check if `status == AVAILABLE`.
    *   Update `Seat` status to `LOCKED`.
    *   Update Redis `seat:status:{seatId}` to `LOCKED`.
    *   *Exception Mapping:* Explicitly catch `ObjectOptimisticLockingFailureException` (JPA version conflict) and map it to a `409 Conflict` HTTP response.
5.  **Order Creation:** Create `Order` in `PENDING` status with `expires_at` set to `created_at + 10 minutes`.
6.  **Release Lock:** Release the Redis lock.

**3.2 Cart Abandonment (Order Expiration)**
*   A `@Scheduled` cron job or a delayed Modulith Event must run periodically to find `Order` records where `status == PENDING` and `expires_at < NOW()`.
*   For these orders:
    *   Update Order `status` to `EXPIRED`.
    *   Update corresponding `Seat` status in MySQL and Redis back to `AVAILABLE`.
    *   Trigger the Waiting List pop (see section 4).

## 4. The Waiting List (Priority Queue)

**The Distributed Approach:**
The Waiting List uses **Redis Sorted Sets (ZSET)**.
*   **Key:** `waitlist:{eventId}:{seatId}`
*   **Score:** Current timestamp (to maintain FIFO priority).
*   **Value:** `userId`
*   **Trigger:** If a user receives a `409 Conflict`, the frontend can prompt them to join the waitlist via `POST /api/v1/events/{eventId}/seats/{seatId}/waitlist`.
*   **Processing:** When an order expires (Cart Abandonment) or payment fails, a Spring Scheduler pops (`ZPOPMIN`) the first user from the Redis Sorted Set, and publishes a notification to that user informing them the seat is available.

## 5. Spring Modulith Event Contracts

To maintain loose coupling between domain modules, use Spring's `ApplicationEventPublisher`.

*   **Event:** `OrderCreatedEvent`
    *   **Payload:** `record OrderCreatedEvent(UUID orderId, Long userId, BigDecimal amount)`
    *   **Published by:** Booking Module.
    *   **Consumed by:** Payment Module (to initialize a payment session).

*   **Event:** `PaymentCompletedEvent`
    *   **Payload:** `record PaymentCompletedEvent(UUID orderId, String status)`
    *   **Published by:** Payment Module.
    *   **Consumed by:** Booking Module (to update Order to CONFIRMED, Seat to BOOKED, and Redis `seat:status:{seatId}` to BOOKED).

## 6. Kafka Topic Schema

For externalized, asynchronous processing (Notifications).

*   **Topic:** `ticket-confirmations`
*   **Producer:** Booking Module (published after receiving `PaymentCompletedEvent` and confirming the order).
*   **Consumer:** Notification Module.
*   **Message Payload (JSON):**
    ```json
    {
      "eventId": 123,
      "orderId": "order-uuid-string",
      "userId": 12345,
      "userEmail": "user@example.com",
      "seatNumber": "A-12",
      "timestamp": "2026-04-10T10:00:00Z"
    }
    ```
*   **Action:** Consumer simulates a 2-second delay, generates a dummy PDF, and logs "Email sent to user@example.com".
