-- ============================================================
-- Clinical Order Management System - Database Initialization
-- ============================================================

-- Create databases for each service
CREATE DATABASE clinical_orders;
CREATE DATABASE clinical_pharmacy;

-- ─── Order Service Schema ──────────────────────────────────
\c clinical_orders;

CREATE TABLE IF NOT EXISTS orders (
    order_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id      UUID NOT NULL,
    doctor_id       UUID NOT NULL,
    order_type      VARCHAR(50) NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    notes           TEXT,
    rejection_reason TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS order_processed_events (
    event_id        VARCHAR(255) PRIMARY KEY,
    processed_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    event_type      VARCHAR(100)
);

CREATE INDEX idx_orders_patient_id  ON orders(patient_id);
CREATE INDEX idx_orders_status      ON orders(status);
CREATE INDEX idx_orders_created_at  ON orders(created_at);

-- ─── Pharmacy Service Schema ───────────────────────────────
\c clinical_pharmacy;

CREATE TABLE IF NOT EXISTS medications (
    medication_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    medication_name VARCHAR(255) NOT NULL UNIQUE,
    stock           INTEGER NOT NULL DEFAULT 0,
    unit            VARCHAR(50),
    description     TEXT
);

CREATE TABLE IF NOT EXISTS reservations (
    reservation_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL UNIQUE,
    medication_id   UUID REFERENCES medications(medication_id),
    quantity        INTEGER NOT NULL DEFAULT 0,
    status          VARCHAR(50) NOT NULL,
    failure_reason  TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS pharmacy_processed_events (
    event_id        VARCHAR(255) PRIMARY KEY,
    processed_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    event_type      VARCHAR(100)
);

CREATE INDEX idx_reservations_order_id ON reservations(order_id);
CREATE INDEX idx_reservations_status   ON reservations(status);

-- Seed initial medication stock
INSERT INTO medications (medication_name, stock, unit, description) VALUES
    ('Amoxicillin',   100, 'tablets', 'Broad-spectrum antibiotic'),
    ('Ibuprofen',     200, 'tablets', 'NSAID anti-inflammatory'),
    ('Metformin',     150, 'tablets', 'Type 2 diabetes medication'),
    ('Lisinopril',     80, 'tablets', 'ACE inhibitor for hypertension'),
    ('Atorvastatin',  120, 'tablets', 'Statin for cholesterol')
ON CONFLICT (medication_name) DO NOTHING;
