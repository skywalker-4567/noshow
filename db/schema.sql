-- Run this once against a fresh hospital_noshow database.
-- ddl-auto=update will manage incremental changes after this point;
-- this file establishes the baseline structure exactly as specified in §4.

CREATE DATABASE IF NOT EXISTS hospital_noshow;
USE hospital_noshow;

CREATE TABLE users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    email      VARCHAR(100) NOT NULL UNIQUE,
    role       ENUM('ADMIN','DOCTOR','RECEPTIONIST') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE patients (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    age           INT NOT NULL,
    gender        ENUM('M','F','OTHER') NOT NULL,
    phone         VARCHAR(15),
    email         VARCHAR(100),
    scholarship   BOOLEAN DEFAULT FALSE,
    hypertension  BOOLEAN DEFAULT FALSE,
    diabetes      BOOLEAN DEFAULT FALSE,
    alcoholism    BOOLEAN DEFAULT FALSE,
    sms_received  BOOLEAN DEFAULT FALSE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE doctors (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    specialization VARCHAR(100),
    user_id        BIGINT UNIQUE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE appointments (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id     BIGINT NOT NULL,
    doctor_id      BIGINT NOT NULL,
    scheduled_date DATE NOT NULL,
    scheduled_time TIME NOT NULL,
    status         ENUM('SCHEDULED','CONFIRMED','COMPLETED','NO_SHOW','CANCELLED')
                   DEFAULT 'SCHEDULED',
    notes          TEXT,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by     VARCHAR(50),
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (doctor_id)  REFERENCES doctors(id)
);

CREATE TABLE risk_scores (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id   BIGINT NOT NULL UNIQUE,
    risk_score       DECIMAL(5,4) NOT NULL,
    risk_level       ENUM('LOW','MEDIUM','HIGH') NOT NULL,
    days_waiting     INT NOT NULL,
    scored_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    model_version    VARCHAR(20) DEFAULT '1.0',
    FOREIGN KEY (appointment_id) REFERENCES appointments(id)
);

CREATE TABLE audit_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id   BIGINT NOT NULL,
    old_status  VARCHAR(50),
    new_status  VARCHAR(50),
    changed_by  VARCHAR(50) NOT NULL,
    changed_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remarks     TEXT
);