This documentation is designed to serve as your project blueprint. It bridges your current background in **Java/Spring Boot** and **PHP/MySQL** with the advanced engineering required for **SDE-1 roles**.

---

# **Project Specification: SwiftTick**

**Subtitle:** A High-Concurrency Distributed Reservation System (Modular Monolith)

## **1\. Project Overview**

**SwiftTick** is a modern, high-performance ticketing engine designed to handle massive traffic spikes (like a flash sale or concert launch). Unlike a standard CRUD app, this project focuses on **concurrency control**, **distributed systems patterns**, and **performance optimization**.

### **Key Objectives**

* **High Availability:** Ensure the system stays up during traffic surges.  
* **Data Integrity:** Prevent over-selling or double-booking using advanced locking.  
* **Scalability:** Use an asynchronous, event-driven approach to decouple heavy tasks.

---

## **2\. Technical Stack**

| Category | Technology | Purpose |
| :---- | :---- | :---- |
| **Language** | Java 21 | Leveraging **Virtual Threads** for high concurrency. |
| **Framework** | Spring Boot 3 | Using **Spring Modulith** for architectural boundaries. |
| **Database** | MySQL | Relational storage for transactions and ACID compliance. |
| **Caching** | Redis | Distributed locking and high-speed seat count lookups. |
| **Messaging** | Apache Kafka | Asynchronous processing for tickets and notifications. |
| **DevOps** | Docker, GitHub Actions | Containerization and automated CI/CD pipelines. |

---

## **3\. Modular Architecture**

We will use a **Modular Monolith** structure. This keeps the code clean and allows you to discuss "Domain-Driven Design" (DDD) in interviews.

### **Domain Modules**

1. **Event Module:** Manages event details, venues, and seat layouts.  
2. **Booking Module:** Handles the core "Reservation" logic and seat allocation.  
3. **Payment Module:** Simulates payment processing and status updates.  
4. **Notification Module:** Consumes Kafka events to send email confirmations and PDFs.

---

## **4\. The 4-Week Development Roadmap**

### **Week 1: Foundation & "The Locking Problem"**

* **Setup:** Initialize Spring Boot with **Spring Modulith**. Verify module boundaries with a unit test.  
* **Database Design:** Create tables for Events, Tickets, and Orders. Implement **Optimistic Locking** (@Version in JPA) to handle simultaneous clicks on the same ticket.  
* **DSA Implementation:** Build a **Token Bucket Rate Limiter** from scratch as a Spring Security Filter to prevent API spamming.

### **Week 2: Speed with Redis & Virtual Threads**

* **Virtual Threads:** Configure your Tomcat executor to use Java 21 Virtual Threads, allowing your app to handle thousands of concurrent requests with low RAM usage.  
* **Redis Integration:** Move the "Seat Inventory" to Redis. Use **Redisson** to implement a **Distributed Lock** for the booking process.  
* **DSA Task:** Use a **Priority Queue (Min-Heap)** to manage a "Waiting List" for users if a ticket becomes temporarily locked.

### **Week 3: Event-Driven Scaling (Kafka)**

* **Internal Events:** Use ApplicationEventPublisher to notify the Payment module when an order is created.  
* **Kafka Producer:** Once payment is successful, publish a message to a Kafka topic named ticket-confirmations.  
* **Kafka Consumer:** Build a separate service logic that listens to Kafka and "processes" the ticket (simulates a 2-second delay to show how async helps performance).

### **Week 4: Production Grade & DevOps**

* **Containerization:** Write a docker-compose.yml to orchestrate Java, MySQL, Redis, and Kafka.  
* **Observability:** Add **Micrometer** and **Prometheus**. Track "p99 latency"—the time it takes for the slowest 1% of users to book a ticket.  
* **CI/CD:** Use **GitHub Actions** to automate builds and run boundary checks.

---

## **5\. Interview "Talking Points"**

When you add this to your resume, you will be prepared to answer:

* *"How did you ensure two people didn't buy the same seat?"* (Answer: Redis Distributed Locking & MySQL Optimistic Locking).  
* *"Why a Monolith instead of Microservices?"* (Answer: To avoid network latency and ensure transactional consistency while maintaining clean boundaries).  
* *"How did you handle 10k users?"* (Answer: Offloading heavy tasks to Kafka and using Java 21 Virtual Threads).