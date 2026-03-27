# SwiftTick

**SwiftTick** is a high-concurrency distributed reservation system built with **Java 21** and **Spring Boot 3**. It leverages the **Modular Monolith** architecture to provide a robust, scalable, and maintainable engine for handling sudden traffic spikes, such as flash sales or concert ticket launches.

## 🚀 Key Features

-   **Modular Monolith Architecture:** Uses **Spring Modulith** to maintain strict domain boundaries while keeping the deployment simple.
-   **High Concurrency Control:** Implements **Redisson Distributed Locks** and **JPA Optimistic Locking** (@Version) to prevent double-booking.
-   **Rate Limiting:** Features a custom-built **Token Bucket Algorithm** implemented as a Spring Security filter to mitigate API abuse.
-   **Asynchronous Processing:** Utilizes **Apache Kafka** for event-driven communication between modules (e.g., triggering notifications after a successful booking).
-   **Idempotency:** Robust handling of network retries using an `Idempotency-Key` header backed by **Redis**.
-   **Waitlist Management:** Fair queuing for high-demand seats using **Redis Sorted Sets**.
-   **Virtual Threads:** Optimized for modern hardware using Java 21's Project Loom to handle thousands of concurrent requests with minimal resource overhead.

## 🛠️ Tech Stack

| Component         | Technology                      |
| :---------------- | :------------------------------ |
| **Language**      | Java 21 (Virtual Threads)       |
| **Framework**     | Spring Boot 3.x                 |
| **Architecture**  | Spring Modulith                 |
| **Database**      | MySQL                           |
| **Caching/Locks** | Redis / Redisson                |
| **Messaging**     | Apache Kafka                    |
| **Documentation** | Swagger / OpenAPI               |

## 📂 Project Structure

-   `com.swifttick.event`: Manages event metadata, seat layouts, and availability.
-   `com.swifttick.booking`: Core reservation logic, seat locking, and order management.
-   `com.swifttick.payment`: Integration point for payment processing (Mocked for MVP).
-   `com.swifttick.notification`: Asynchronous handlers for emails and tickets.
-   `com.swifttick.common`: Shared cross-cutting concerns (Security, Rate Limiting, Filters).

## 🚦 Getting Started

### Prerequisites
- Docker & Docker Compose
- JDK 21+
- Maven 3.9+

### Quick Start
1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/SwiftTick.git
   cd SwiftTick
   ```

2. **Spin up Infrastructure (MySQL, Redis, Kafka):**
   ```bash
   docker-compose up -d
   ```

3. **Run the Application:**
   ```bash
   mvn spring-boot:run
   ```

## 📜 Documentation
Detailed design documents can be found in the `/Project` directory:
- [PRD.md](./Project/PRD.md) - Product Requirements
- [TDD.md](./Project/TDD.md) - Technical Specifications
- [Description.md](./Project/Description.md) - Project Overview
