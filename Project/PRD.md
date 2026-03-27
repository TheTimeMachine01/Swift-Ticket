# Product Requirements Document (PRD): SwiftTick

## 1. Product Overview
**Project Name:** SwiftTick
**Subtitle:** A High-Concurrency Distributed Reservation System (Modular Monolith)

**1.1 Purpose**
SwiftTick is a modern, high-performance ticketing engine engineered to handle massive, sudden traffic spikes typically seen during flash sales or highly anticipated concert launches. It is designed to be a robust alternative to standard CRUD applications by focusing heavily on concurrency control, distributed systems patterns, and performance optimization.

**1.2 Key Objectives**
* **High Availability:** Ensure the system remains responsive and operational during extreme traffic surges.
* **Data Integrity:** Guarantee no over-selling or double-booking of seats using advanced locking mechanisms.
* **Scalability:** Utilize an asynchronous, event-driven architecture to decouple heavy, blocking tasks from the main request flow.

---

## 2. Architecture & Technology Stack

**2.1 Architectural Pattern**
* **Modular Monolith:** The application will be structured as a Modular Monolith using **Spring Modulith**. This approach ensures clean, Domain-Driven Design (DDD) boundaries, avoiding the network latency of microservices while maintaining transactional consistency and strict module separation.

**2.2 Tech Stack**
| Component | Technology | Rationale |
| :--- | :--- | :--- |
| **Language** | Java 21 | Utilizes **Virtual Threads** for handling high concurrency with low resource overhead. |
| **Framework** | Spring Boot 3 | Core application framework; leverages Spring Modulith for architectural boundaries. |
| **Database** | MySQL | Primary relational storage ensuring ACID compliance for transactions and orders. |
| **Caching/Locking** | Redis | In-memory datastore for high-speed seat count lookups and distributed locking. |
| **Messaging** | Apache Kafka | Event streaming platform for asynchronous processing (e.g., tickets, notifications). |
| **DevOps/Infra** | Docker, GitHub Actions | Containerization for consistent environments and automated CI/CD pipelines. |
| **Observability** | Micrometer, Prometheus | For tracking metrics like p99 latency. |

---

## 3. Features & Functional Requirements
*Note: For all detailed Entity mappings, API Contracts, and Kafka JSON Schemas, refer to the accompanying `TDD.md` document.*

The system is divided into four primary domain modules:

**3.1 Event Module**
* Manage event details, venues, and seat layouts.
* Provide fast read access to event availability (cached in Redis).

**3.2 Booking Module**
* Handle core reservation logic and seat allocation via strictly defined REST endpoints.
* Implement robust concurrency controls (Redisson + JPA @Version) to prevent double-booking.
* Handle API idempotency using an `Idempotency-Key` header to prevent duplicate orders from network retries.
* **Waiting List:** Provide a mechanism for users to join a waitlist if a ticket is locked. This must be implemented using Redis Sorted Sets to ensure consistency across distributed instances.

**3.3 Payment Module**
* Simulate payment processing securely.
* Listen for `OrderCreatedEvent` internal Modulith events.
* Update order statuses and publish `PaymentCompletedEvent` upon success.

**3.4 Notification Module**
* Consume asynchronous Kafka events on the `ticket-confirmations` topic.
* Send simulated email confirmations and generate/distribute PDF tickets without blocking the main booking thread.

---

## 4. Non-Functional Requirements & System Design

**4.1 Concurrency & Data Integrity**
* **Idempotency:** The booking endpoint must gracefully handle duplicate network requests via `Idempotency-Key` caching.
* **Distributed Locking (Redis):** Implement Redis-based distributed locks (using Redisson) for the critical booking process. Wait 2 seconds max for acquisition, lease for 10 seconds.
* **Optimistic Locking (MySQL):** Utilize JPA `@Version` on MySQL `Seat` entities as a secondary defense to handle simultaneous database updates safely.

**4.2 Performance & Scalability**
* **Virtual Threads:** Configure Tomcat to use Java 21 Virtual Threads, enabling the system to process thousands of concurrent requests with minimal RAM consumption.
* **Rate Limiting:** Build a custom Token Bucket Rate Limiter from scratch as a Spring Security Filter. This will key off an `X-User-ID` mocked authentication header to protect APIs.
* **Event-Driven Decoupling:** Use `ApplicationEventPublisher` for internal domain events and Apache Kafka for decoupled, cross-boundary asynchronous processing.

**4.3 Observability & DevOps**
* Track and monitor "p99 latency" (the response time for the slowest 1% of users).
* Integrate Micrometer and Prometheus for comprehensive application metrics.
* Containerize the entire stack using Docker Compose.

---

## 5. Implementation Roadmap (4 Weeks)

**Week 1: Foundation & "The Locking Problem"**
* **Setup:** Initialize Spring Boot with Spring Modulith. Write unit tests to verify module boundaries.
* **Database Design:** Implement precise schemas as defined in the TDD.
* **Security/DSA:** Implement the Token Bucket Rate Limiter based on `X-User-ID`.
* **Idempotency:** Implement basic Redis-based idempotency filters for endpoints.

**Week 2: Speed & Scalability**
* **Concurrency:** Enable Java 21 Virtual Threads in the Tomcat executor.
* **Locking Flow:** Implement Redisson Distributed Locks and JPA Optimistic Locking for the booking flow exactly as specified in the TDD.
* **Waitlist (DSA):** Implement the Redis Sorted Set (ZSET) Waiting List.

**Week 3: Event-Driven Architecture**
* **Internal Events:** Implement `ApplicationEventPublisher` event records as defined in the TDD to orchestrate the Order -> Payment flow.
* **Message Broker:** Setup Apache Kafka.
* **Kafka Pipeline:** Publish messages with strict JSON schemas to `ticket-confirmations` topic upon successful payment. Build a consumer with a 2-second delay simulation.

**Week 4: Production Readiness & DevOps**
* **Containerization:** Create a `docker-compose.yml` orchestrating the Java app, MySQL, Redis, and Kafka.
* **Monitoring:** Add Micrometer and Prometheus integration; set up p99 latency tracking.
* **CI/CD:** Configure GitHub Actions for automated builds and boundary verification checks.

---

## 6. Success Criteria
* The application successfully maps to the exact Data Models and API contracts in `TDD.md`.
* The system prevents double bookings through the defined Redis + MySQL locking flow.
* API endpoints are effectively protected by the custom rate limiter and idempotency guards.
* The Waiting list operates correctly in a distributed setup (using Redis, not local memory).
* Main booking flow latency remains consistently low, with notifications offloaded to Kafka.
* The modular monolith boundaries are strictly maintained and continuously verified by automated CI/CD tests.