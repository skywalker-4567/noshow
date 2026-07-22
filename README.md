# 🏥 Hospital Appointment No-Show Risk Engine

A full-stack clinical decision support system that predicts patient no-show risk for hospital appointments using machine learning, enabling proactive intervention before missed appointments occur.

---

## 📌 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [System Architecture](#system-architecture)
- [Tech Stack](#tech-stack)
- [Machine Learning Model](#machine-learning-model)
- [Screenshots](#screenshots)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Design Decisions](#design-decisions)
- [Project Structure](#project-structure)

---

## Overview

Hospital appointment no-shows are a significant operational challenge — missed slots waste clinical capacity, delay care for other patients, and cost healthcare systems billions annually. This system addresses the problem by scoring every upcoming appointment with a risk level (LOW / MEDIUM / HIGH) the night before, so receptionists can prioritise follow-up calls for high-risk patients.

The system was trained on **110,527 real patient records** from a Brazilian public health dataset (Kaggle), using a Random Forest classifier with class-balanced weighting to handle the natural 80/20 class imbalance between shows and no-shows.

---

## Key Features

### Risk Scoring
- Nightly batch scoring of all next-day appointments via a scheduled job
- Each appointment scored by a Python/Flask ML microservice and labelled LOW, MEDIUM, or HIGH
- Graceful fallback to rule-based scoring if the ML service is unreachable — system never goes down
- Risk badges visible across all relevant views

### Role-Based Access Control
Three distinct roles, each with its own dashboard and restricted routes:

| Role | Access |
|---|---|
| **ADMIN** | Full system — dashboard, all appointments, patients, audit log, risk report, scheduler triggers |
| **RECEPTIONIST** | Book appointments, manage status transitions (confirm, complete, no-show, cancel) |
| **DOCTOR** | Read-only view of today's personal schedule with risk badges |

### Audit Trail
Every status change on every appointment is logged with the actor, timestamp, old status, and new status — providing a complete, tamper-evident history.

### Async Email Notifications
Six email triggers fire asynchronously after transaction commit, so email failures never affect the main request flow:
- Appointment confirmation to patient
- Status update to patient
- No-show notification to admin
- HIGH-risk alert to all receptionists
- Morning digest to receptionists
- Weekly accuracy report to admin

### Admin Dashboard
- Live stat cards (total appointments, today's count, HIGH risk count, total patients)
- Chart.js visualisations — appointments by status (bar) and risk level distribution (doughnut)
- Manual "Run Now" buttons to trigger any scheduled job instantly for demo purposes

### Weekly Accuracy Report
Tracks model performance over a rolling 7-day window — predicted HIGH count, actual no-show count, true positives, and precision — so the model's real-world effectiveness is visible without needing a data science tool.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Browser (Thymeleaf)                   │
│         Admin · Receptionist · Doctor dashboards         │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP (port 8080)
┌──────────────────────▼──────────────────────────────────┐
│              Spring Boot 3.2 Application                 │
│                                                          │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────┐  │
│  │ Controllers │  │   Services   │  │  Repositories  │  │
│  │  (5 roles)  │  │ @Transact'l  │  │  (JPA/Postgres)│  │
│  └─────────────┘  └──────┬───────┘  └────────────────┘  │
│                           │                              │
│  ┌────────────────────────▼──────────────────────────┐   │
│  │         Spring Security + JWT Filter Chain        │   │
│  │    Session-first → Bearer header fallback         │   │
│  └───────────────────────────────────────────────────┘   │
│                           │                              │
│  ┌────────────────────────▼──────────────────────────┐   │
│  │    HospitalScheduler (@Scheduled cron jobs)       │   │
│  │  21:00 batch scoring · 08:00 alerts · Mon 07:00   │   │
│  └────────────────────────┬──────────────────────────┘   │
└───────────────────────────┼─────────────────────────────┘
                            │ HTTP POST /predict (port 5001)
              ┌─────────────▼──────────────┐
              │   Flask ML Microservice     │
              │   Random Forest Classifier  │
              │   trained on 110K records   │
              └─────────────────────────────┘
                            │
              ┌─────────────▼──────────────┐
              │       PostgreSQL DB         │
              │  6 tables · full schema     │
              └─────────────────────────────┘
```

**Key architectural choices:**
- **Loose coupling** between Spring Boot and Flask — if Flask is unreachable, a rule-based fallback scores the appointment automatically. The batch never fails silently.
- **`@Transactional` at the service method level**, never on the scheduler loop — a failure on one appointment rolls back only that record, not the entire night's scoring.
- **`@Async` email dispatch** fires after transaction commit, ensuring emails are never sent for changes that rolled back.

---

## Tech Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Spring Boot | 3.2.4 | Application framework |
| Spring Security | 6.2.3 | Auth, role-based access control |
| Spring Data JPA | 3.2.4 | ORM and repository layer |
| jjwt | 0.12.3 | JWT generation and validation |
| SpringDoc OpenAPI | 2.3.0 | Swagger UI and API docs |
| JavaMailSender | 6.1.5 | Async email via Gmail SMTP |
| Thymeleaf | 3.1.2 | Server-side templating |
| Lombok | latest | Boilerplate reduction |

### Database
| Technology | Purpose |
|---|---|
| PostgreSQL 18 | Primary database (6 tables) |
| Hibernate ORM 6.4 | Schema management (`ddl-auto=update`) |
| HikariCP | Connection pooling |

### ML Microservice
| Technology | Version | Purpose |
|---|---|---|
| Python | 3.11+ | Runtime |
| Flask | 3.1.0 | REST API server |
| scikit-learn | 1.6.1 | Random Forest classifier |
| pandas | 2.2.3 | Data loading and feature engineering |
| joblib | 1.4.2 | Model serialisation |

### Frontend
| Technology | Purpose |
|---|---|
| Thymeleaf + Bootstrap 5.3 | Responsive UI, role-aware templates |
| Chart.js 4.4 | Dashboard visualisations |
| `thymeleaf-extras-springsecurity6` | `sec:authorize` for role-conditional rendering |

---

## Machine Learning Model

### Dataset
**Kaggle — Medical Appointment No Shows**
- Source: Brazilian public health system (SUS), Vitória, Brazil
- Size: 110,527 patient records
- Target: `No-show` — whether a patient missed their appointment
- Class balance: ~80% showed up, ~20% no-showed

### Features Used
| Feature | Source | Notes |
|---|---|---|
| Age | Patient record | Continuous |
| Gender | Patient record | F=0, M=1 (Female is majority class at ~65%) |
| Scholarship | Patient record | Government welfare recipient |
| Hypertension | Patient record | Chronic condition flag |
| Diabetes | Patient record | Chronic condition flag |
| Alcoholism | Patient record | Chronic condition flag |
| SMS Received | Patient record | Whether a reminder was sent |
| Days Waiting | Derived | `abs(AppointmentDay - ScheduledDay)` |

### Model Details
```python
RandomForestClassifier(
    n_estimators=100,
    random_state=42,
    class_weight='balanced'   # critical for minority class recall
)
```

**Why `class_weight='balanced'`:** Without it, the model achieves 80% accuracy by predicting "show" for every patient — useless for intervention. With balanced weights, no-show recall improves from **0.19 → 0.39**, catching nearly twice as many genuine no-shows.

### Measured Performance
| Metric | Without Balancing | With Balancing |
|---|---|---|
| Accuracy | 77% | 71% |
| No-show Recall | 0.19 | **0.39** |
| No-show Precision | 0.35 | 0.31 |

### Risk Thresholds
Thresholds were **calibrated against the model's actual test-set probability distribution**, not assumed from benchmarks:

| Threshold | Label | No-show Rate in Bucket | % of Population |
|---|---|---|---|
| prob ≥ 0.50 | HIGH | 31.3% (vs 20.1% base) | 25% |
| prob 0.30–0.50 | MEDIUM | ~20% (near baseline) | 19% |
| prob < 0.30 | LOW | 14.5% (below baseline) | 56% |

HIGH captures **39% of all no-shows** in just **25% of the patient population** — a meaningful 1.56× lift over random selection.

### Fallback Rules (Flask-down scenario)
```
daysWaiting > 15 AND NOT smsReceived AND (hypertension OR diabetes) → HIGH
daysWaiting > 7                                                      → MEDIUM
otherwise                                                            → LOW
```

---

## Screenshots

### Admin Dashboard
Stat cards, Chart.js visualisations, and manual scheduler triggers all on one page.

### Appointments with Risk Badges
Paginated table with colour-coded risk badges (HIGH/MEDIUM/LOW) and status filter dropdown.

### Audit Log
Complete tamper-evident trail of every status transition, sorted newest-first.

### Receptionist — Book Appointment
Validated booking form with patient and doctor dropdowns, date/time pickers, and inline error messages.

### Risk Accuracy Report
Weekly precision metrics — predicted HIGH, actual no-shows, true positives, and calculated precision with a plain-English interpretation note.

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- Python 3.11+

### 1. Database Setup

Create the database in PostgreSQL (pgAdmin or psql):
```sql
CREATE DATABASE hospital_noshow;
```
Hibernate will auto-create all 6 tables on first startup via `ddl-auto=update`.

### 2. Configure application.properties

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/hospital_noshow
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD

# JWT — generate with: openssl rand -hex 32
jwt.secret=YOUR_64_CHAR_HEX_SECRET
jwt.expiration=86400000

# Email (Gmail App Password — not your login password)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_GMAIL@gmail.com
spring.mail.password=YOUR_16_CHAR_APP_PASSWORD
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### 3. Train the ML Model

Download the dataset from [Kaggle](https://www.kaggle.com/datasets/joniarroba/noshowappointments) and place `KaggleV2-May-2016.csv` in `flask-ml/`.

```bash
cd flask-ml
python -m venv venv

# Mac/Linux
source venv/bin/activate

# Windows
venv\Scripts\activate

pip install -r requirements.txt
python train.py
# Expect: recall for class 1 (no-show) ≈ 0.39
# Output: noshow_model.pkl (~190MB)
```

### 4. Start the ML Service

```bash
# Keep this terminal open
python app.py
# Running on http://127.0.0.1:5001
```

Verify:
```bash
curl http://localhost:5001/health
# {"status": "UP", "model": "loaded"}
```

### 5. Start Spring Boot

```bash
cd hospital-noshow
mvn spring-boot:run
# Started NoshowApplication in ~10 seconds
```

On first startup, `DataSeeder` automatically creates:
- 3 users: `admin/admin123`, `receptionist/rec123`, `doctor/doc123`
- 1 doctor: Dr. Meera Sharma (General Medicine)
- 4 patients with varying risk profiles

### 6. Open the App

```
http://localhost:8080/login
```

> **Important:** Start Flask before Spring Boot if you want the ML model to score appointments. If Flask is down during scoring, rule-based fallback fires automatically — check `risk_scores.model_version` to distinguish (`1.0` = real model, `fallback-rules` = fallback).

---

## API Documentation

Swagger UI is available at:
```
http://localhost:8080/swagger-ui.html
```

Bearer token authentication is supported for direct API calls (useful for testing without a browser session). The JWT filter checks the session first, then falls back to the `Authorization: Bearer <token>` header — this means Swagger "Try it out" works for any endpoint after authorising with a token.

### Key Endpoints

#### Auth
| Method | Path | Description |
|---|---|---|
| GET | `/login` | Login page |
| POST | `/auth/login` | Authenticate, receive JWT in session |
| POST | `/auth/logout` | Invalidate session |

#### Admin
| Method | Path | Description |
|---|---|---|
| GET | `/admin/dashboard` | Stats + charts + scheduler triggers |
| GET | `/admin/appointments` | Paginated appointments with risk badges |
| GET | `/admin/patients` | Patient CRUD table |
| GET | `/admin/audit-log` | Full audit trail |
| GET | `/admin/risk-report` | Weekly accuracy metrics |

#### Receptionist
| Method | Path | Description |
|---|---|---|
| GET | `/receptionist/dashboard` | Today's appointments + HIGH-risk alert |
| GET | `/receptionist/appointments/new` | Booking form |
| POST | `/receptionist/appointments/new` | Save appointment |
| POST | `/receptionist/appointments/{id}/confirm` | SCHEDULED → CONFIRMED |
| POST | `/receptionist/appointments/{id}/no-show` | → NO_SHOW |
| POST | `/receptionist/appointments/{id}/complete` | → COMPLETED |
| POST | `/receptionist/appointments/{id}/cancel` | → CANCELLED |

#### Scheduler (Admin only — manual triggers)
| Method | Path | Description |
|---|---|---|
| POST | `/admin/scheduler/run-scoring` | Triggers nightly batch scoring |
| POST | `/admin/scheduler/run-morning-alerts` | Sends morning HIGH-risk digest |
| POST | `/admin/scheduler/run-weekly-report` | Sends weekly accuracy email |

#### Flask ML Service
| Method | Path | Description |
|---|---|---|
| POST | `localhost:5001/predict` | Score one appointment |
| GET | `localhost:5001/health` | Service health check |

---

## Design Decisions

### Why a separate Flask microservice?
Spring Boot and the ML model are **loosely coupled by design**. The model can be retrained, replaced, or scaled independently without touching the Java codebase. If Flask is unreachable, a deterministic fallback kicks in — the scheduling system never blocks on ML availability.

### Why `@Transactional` on the service method, not the scheduler loop?
If the entire batch loop ran in one transaction, a failure on appointment #47 would roll back appointments 1–46. Per-record transactions isolate failures — one bad record is logged and skipped, the rest of the batch completes.

### Why JWT stored in the session instead of returned to the client?
This app uses Thymeleaf (server-rendered HTML), not a SPA. The session is the natural auth carrier for browser flows. The JWT is still generated and validated — it's just stored server-side in the session. API callers (Swagger, REST clients) fall back to the `Authorization` header, so both flows work.

### Why not Docker/Redis/Kafka?
The brief explicitly required a demo that runs entirely in the browser without infrastructure dependencies. The trade-off is simplicity of setup and demonstration — the architecture uses the same patterns (async processing, service decoupling, scheduled jobs) that would be present in a production system, just with simpler infrastructure.

---

## Project Structure

```
hospital-noshow/
│
├── src/main/java/com/hospital/noshow/
│   ├── config/          # Security, Async, Swagger, RestTemplate
│   ├── controller/      # Auth, Admin, Receptionist, Doctor, Scheduler
│   ├── dto/             # AppointmentDTO, PatientDTO, RiskPrediction*, DashboardStats
│   ├── entity/          # User, Patient, Doctor, Appointment, RiskScore, AuditLog
│   ├── enums/           # AppointmentStatus, RiskLevel, UserRole, Gender
│   ├── exception/       # GlobalExceptionHandler, ResourceNotFoundException
│   ├── repository/      # 6 JPA repositories
│   ├── scheduler/       # HospitalScheduler (3 cron jobs)
│   ├── security/        # JwtUtil, JwtAuthFilter, UserDetailsServiceImpl
│   ├── seeder/          # DataSeeder (CommandLineRunner with count() guard)
│   └── service/         # 6 service interfaces + implementations
│
├── src/main/resources/
│   ├── application.properties
│   └── templates/
│       ├── fragments/   # header.html, navbar.html (role-aware)
│       ├── admin/       # dashboard, appointments, patients, audit-log, risk-report
│       ├── receptionist/# dashboard, appointment-form, appointment-list
│       ├── doctor/      # dashboard
│       ├── login.html
│       └── error.html
│
├── flask-ml/
│   ├── app.py           # Flask REST API (/predict, /health)
│   ├── train.py         # Model training script
│   ├── model.py         # Load + predict with calibrated thresholds
│   ├── features.py      # Feature encoding and validation
│   ├── requirements.txt
│   └── noshow_model.pkl # Generated by train.py (not committed)
│
└── db/
    └── schema.sql       # Baseline schema (reference only — Hibernate manages it)
```

---

## Database Schema

```
users ──────── doctors (1:1 via user_id)
  │
  └── (context only)

patients ──────── appointments ──────── risk_scores (1:1)
                       │
                       └──── audit_logs (1:N via entity_id)
```

Six tables: `users`, `patients`, `doctors`, `appointments`, `risk_scores`, `audit_logs`.

---

*Built with Spring Boot 3, Python/Flask, PostgreSQL, and scikit-learn.*
