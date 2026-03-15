# Distributed Clinical Order Management System

A production-grade event-driven microservices system simulating a real hospital clinical order workflow.

---

## Architecture

```
┌────────────────┐     POST /api/orders     ┌──────────────────────┐
│    Client      │ ───────────────────────► │   Order Service      │
│  (Postman etc) │                          │   :8081              │
└────────────────┘                          └──────────┬───────────┘
                                                       │
                                         Kafka: clinical-order-created
                                                       │
                              ┌────────────────────────┼────────────────────────┐
                              ▼                                                 ▼
                 ┌────────────────────────┐                     ┌────────────────────────┐
                 │   Pharmacy Service     │                     │  Notification Service  │
                 │   :8082               │                     │  :8083                 │
                 │                        │                     │                        │
                 │  - Reserves medication │                     │  - Logs all events     │
                 │  - Manages stock       │                     │  - GET /api/notifications│
                 └────────────┬───────────┘                     └────────────────────────┘
                              │
                Kafka: medication-reserved
                              │
                              ▼
                 ┌────────────────────────┐
                 │   Order Service        │
                 │   (consumer)           │
                 │  PENDING → CONFIRMED   │
                 │  PENDING → REJECTED    │
                 └────────────────────────┘
```

## Services

| Service              | Port | Responsibility                          |
|---------------------|------|-----------------------------------------|
| order-service        | 8081 | Create & manage clinical orders         |
| pharmacy-service     | 8082 | Reserve medications, manage stock       |
| notification-service | 8083 | Observe all events, log notifications   |
| Kafka UI             | 8080 | Browse topics & messages (browser)      |

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose

---

## Quick Start

### 1. Start Infrastructure

```bash
docker-compose up -d
```

Wait ~20 seconds for Kafka and PostgreSQL to be healthy.

### 2. Create Kafka Topics

```bash
chmod +x scripts/create-topics.sh
./scripts/create-topics.sh
```

> Topics are also auto-created by Kafka on first message — the script ensures proper partition counts.

### 3. Build All Services

```bash
mvn clean package -DskipTests
```

### 4. Run Each Service

Open three terminal tabs:

```bash
# Terminal 1 - Order Service
cd order-service
mvn spring-boot:run

# Terminal 2 - Pharmacy Service
cd pharmacy-service
mvn spring-boot:run

# Terminal 3 - Notification Service
cd notification-service
mvn spring-boot:run
```

---

## Configuration

### Using Local PostgreSQL via Docker

Default config works out of the box with docker-compose:

- Order Service DB:    `jdbc:postgresql://localhost:5432/clinical_orders`
- Pharmacy Service DB: `jdbc:postgresql://localhost:5432/clinical_pharmacy`

### Using Supabase (Cloud PostgreSQL)

Set environment variables before running:

```bash
export DB_URL=jdbc:postgresql://<supabase-host>:5432/<db-name>
export DB_USERNAME=<your-username>
export DB_PASSWORD=<your-password>
mvn spring-boot:run
```

---

## API Reference

### Order Service — `http://localhost:8081`

#### Create an Order
```http
POST /api/orders
Content-Type: application/json

{
  "patientId": "11111111-1111-1111-1111-111111111111",
  "doctorId":  "22222222-2222-2222-2222-222222222222",
  "orderType": "MEDICATION",
  "notes": "Prescribed for infection"
}
```

Valid `orderType` values: `MEDICATION`, `LAB_TEST`, `IMAGING`, `PROCEDURE`, `CONSULTATION`

#### Get Order by ID
```http
GET /api/orders/{orderId}
```

#### Get All Orders
```http
GET /api/orders
```

#### Get Orders by Patient
```http
GET /api/orders/patient/{patientId}
```

#### Cancel an Order
```http
PATCH /api/orders/{orderId}/cancel
```

---

### Pharmacy Service — `http://localhost:8082`

#### List All Medications & Stock
```http
GET /api/pharmacy/medications
```

#### List All Reservations
```http
GET /api/pharmacy/reservations
```

#### Get Reservation for an Order
```http
GET /api/pharmacy/reservations/order/{orderId}
```

---

### Notification Service — `http://localhost:8083`

#### View All Notification Events
```http
GET /api/notifications
```

---

## Kafka Topics

| Topic                    | Producer         | Consumer(s)                          |
|--------------------------|------------------|--------------------------------------|
| `clinical-order-created` | order-service    | pharmacy-service, notification-service |
| `medication-reserved`    | pharmacy-service | order-service, notification-service  |

### View Topics in Browser
Open Kafka UI: http://localhost:8080

---

## Key Design Decisions

### Idempotent Consumers
Every consumer checks a `processed_events` table before processing. If a Kafka message is redelivered, it will be silently skipped — preventing duplicate stock deductions or duplicate status updates.

### Pessimistic Locking
`MedicationRepository.findByIdWithLock()` uses `PESSIMISTIC_WRITE` to prevent race conditions when multiple orders arrive simultaneously for the same medication.

### No Type Headers
`spring.json.use.type.headers=false` prevents deserialization failures when services have different package structures. Each consumer specifies its target class explicitly.

### Service Isolation
Each service has its own database. Services communicate only through Kafka events — never direct HTTP calls.

---

## Flow Walkthrough

1. Client sends `POST /api/orders` with `orderType: MEDICATION`
2. Order Service saves order with status `PENDING`, publishes `ClinicalOrderCreatedEvent`
3. Pharmacy Service receives the event, checks stock, reserves medication
4. Pharmacy Service publishes `MedicationReservedEvent` with status `RESERVED` or `FAILED`
5. Order Service receives result, updates status to `CONFIRMED` or `REJECTED`
6. Notification Service receives both events and logs notifications
7. Client polls `GET /api/orders/{orderId}` to check final status

---

## Health Checks

```http
GET http://localhost:8081/actuator/health
GET http://localhost:8082/actuator/health
GET http://localhost:8083/actuator/health
```

---

## Extending the System

### Add a Lab Service
1. Create `lab-service` module (copy pharmacy-service structure)
2. Consume `clinical-order-created` where `orderType = LAB_TEST`
3. Publish `LabResultEvent` to new topic `lab-result-ready`
4. Order Service consumes that topic and updates order status

### Add Redis Caching
Add `spring-boot-starter-data-redis` to order-service, cache `GET /api/orders/{id}` with `@Cacheable`.

### Add Dead Letter Queue handling
Configure `DeadLetterPublishingRecoverer` in KafkaConfig to route failed messages to `*.DLT` topics automatically.
