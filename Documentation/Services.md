# Distributed Clinical Order Management System  
## Database Schema & Table Summary

---

# 🏥 1️⃣ Order Service  
**Schema:** `order_service`  

## Tables

### 1. orders
Main order entity (Prescription or Lab).

**Purpose:**
- Stores patient and doctor information
- Defines order type (PRESCRIPTION / LAB)
- Tracks order status
- Entry point of the entire workflow
- Contains `version` column for optimistic locking (concurrency control)

---

### 2. order_events
Stores domain events related to orders.

**Purpose:**
- Tracks events like `OrderCreated`, `OrderFailed`
- Helps in debugging and auditing
- Supports event-driven architecture visibility

---

# 💊 2️⃣ Pharmacy Service  
**Schema:** `pharmacy_service`

## Tables

### 1. medications
Medication inventory table.

**Purpose:**
- Stores medicine details
- Tracks available stock
- Uses `version` column for concurrency control

---

### 2. reservations
Tracks medication reservations per order.

**Purpose:**
- Reserves medication when prescription order is received
- Stores reservation status (RESERVED / RELEASED / FAILED)
- Used in Saga compensation flow

---

### 3. processed_events
Stores consumed event IDs.

**Purpose:**
- Prevents duplicate event processing
- Ensures idempotent Kafka consumer behavior

---

# 🧪 3️⃣ Lab Service  
**Schema:** `lab_service`

## Tables

### 1. lab_tests
Available lab tests.

**Purpose:**
- Stores test information
- Tracks available slots / capacity

---

### 2. lab_orders
Scheduled lab tests per order.

**Purpose:**
- Links lab test to order
- Tracks scheduling status
- Stores scheduled time

---

### 3. processed_events
Prevents duplicate event handling.

**Purpose:**
- Ensures idempotent event consumption

---

# 💳 4️⃣ Billing Service  
**Schema:** `billing_service`

## Tables

### 1. billing_records
Stores billing transactions.

**Purpose:**
- Links billing to order
- Stores payment amount
- Tracks status (PENDING / COMPLETED / FAILED)
- Supports Saga transaction flow

---

### 2. processed_events
Tracks consumed event IDs.

**Purpose:**
- Prevents double charging
- Ensures safe event reprocessing

---

# 🔔 5️⃣ Notification Service  
**Schema:** `notification_service`

## Tables

### 1. notifications
Stores notification records.

**Purpose:**
- Simulates SMS / Email notifications
- Tracks delivery status
- Linked to order lifecycle

---

### 2. processed_events
Ensures idempotent notification sending.

**Purpose:**
- Prevents duplicate notifications
- Maintains event-processing integrity

---

# 🧠 Architectural Design Principles Demonstrated

- Schema isolation per microservice
- Event-driven architecture support
- Saga pattern compatibility (reservation + compensation)
- Idempotent Kafka consumers
- Optimistic locking for concurrency control
- Audit-friendly event tracking
- Production-oriented database design

---

**Author:** Soham  
**Project:** Distributed Clinical Order Management System  