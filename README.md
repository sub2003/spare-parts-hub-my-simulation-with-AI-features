Live website- 'https://sparepartshub.store'
# Spare Parts Hub

A web-based Computer Spare Parts Management System built to digitize inventory tracking, point-of-sale, warranty management, and supplier operations for a retail hardware store — replacing manual and spreadsheet-based tracking with a centralized web application.

**Course:** SE2030 – Software Engineering, SLIIT, Year 2 Semester 1 (2026)
**Group:** 2026-Y2-S1-MLB-B12G1-02

## Team

| Name                   | Registration No. |
|------------------------|---|
| Siriwardhana P.K.S.M.  | IT25101412 |
| Dilshan S.D.T.I.       | IT25102210 |
| Thaneshh U.            | IT25103161 |
| Nuwandhi N.A.C.        | IT25100446 |
| Rathnayaka R.M.P.S.    | IT25102567 |
| Aththanayaka A.B.C.S.R. | IT25100034 |

## Tech Stack

- **Backend:** Spring Boot (Java), Spring Data JPA, Spring Security
- **Database:** MySQL
- **Frontend:** Thymeleaf + Bootstrap
- **QR Codes:** ZXing

## Core Modules

1. Inventory Storage Location Tracking
2. Real-Time Product Search & Automated Checkout
3. Dynamic Urgency Score Tracking
4. Warranty and Returns Management
5. Supplier Management
6. Centralized Reporting, Audit Logging & External Supplier Portal

## Getting Started

### Prerequisites
- Java 17+
- Maven
- MySQL Server (running locally)
- IntelliJ IDEA (recommended)

### 1. Clone the repository
```bash
git clone https://github.com/sub2003/spare-parts-hub-my-simulation.git
```

### 2. Set up the database
Open MySQL Workbench (or the MySQL CLI), connect to your local server, and run the full schema file: database/schema.sql

This creates the `spare_parts_db` database along with all 19 tables.

### 3. Configure the connection
In `src/main/resources/application.properties`, set your local MySQL credentials:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/spare_parts_db
spring.datasource.username=root
spring.datasource.password=your_password
```

### 4. Run the project
Open the project in IntelliJ and run the main application class, or via terminal:
```bash
./mvnw spring-boot:run
```

## Optional AI Demand Forecasting demo

Function 6 Reporting includes an optional Python/FastAPI demand forecasting service. The initial 18-month history is synthetic demo data (seed 42); new real POS sales are incorporated during retraining. See `docs/ai-demand-forecasting-run-guide.md` and `docs/ai-demand-forecasting.md`.
