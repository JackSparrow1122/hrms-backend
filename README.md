# GA-HRMS Backend Service

Production-grade Human Resource Management System backend built with **Spring Boot 3.4.x**, **Java 21 (LTS)**, **PostgreSQL 16**, **Flyway**, and **Spring Security 6 + JJWT**.

Developed strictly in accordance with [HRMS Production PRD v1.1](file:///c:/Users/AjayPawar/Desktop/code/ga-hrms/docs/HRMS_Production_PRD_v1.1_Scope_Matrix.pdf).

---

## 🛠 Technology Stack

- **Java Version:** OpenJDK 21 LTS
- **Framework:** Spring Boot 3.4.1
- **Database:** PostgreSQL 16
- **Database Migrations:** Flyway (`src/main/resources/db/migration`)
- **Authentication & Security:** Spring Security 6 + JJWT (RFC 7519) + BCrypt
- **API Documentation:** OpenAPI 3 / Swagger UI (`springdoc-openapi`)
- **Build Tool:** Apache Maven Wrapper (`mvnw` / `mvnw.cmd`)
- **Containerization:** Docker & Docker Compose

---

## 📁 Project Architecture & Package Structure

```
com.gahrms.backend
├── GahrmsBackendApplication.java
├── config
│   ├── SecurityConfig.java          # Spring Security 6 filter chain, CORS, CSRF, BCrypt
│   ├── JwtAuthenticationFilter.java # Stateless Bearer token extraction and authentication
│   ├── OpenApiConfig.java           # Swagger 3 & Bearer auth security scheme definition
│   ├── JpaAuditConfig.java          # Auditing entity listener (AuditorAware)
│   ├── CorsConfig.java              # CORS configuration for Vite frontend (:5173)
│   └── JacksonConfig.java           # JavaTimeModule & UTC ISO-8601 formatting
├── common
│   ├── dto                          # Standard envelopes (ApiResponse, ErrorResponse, PageResponse)
│   ├── entity                       # BaseEntity (id, createdAt, updatedAt, createdBy, updatedBy, version)
│   ├── exception                    # GlobalExceptionHandler and specific ApiException subclasses
│   └── util                         # AppConstants, SecurityUtils
└── modules
    ├── identity                     # FR-IAM: User, Role, RefreshToken, AuthController, AuthService, JwtService
    ├── organization                 # FR-ORG: Company, Branch, Department, Designation
    ├── employee                     # FR-EMP: Employee, EmploymentStatus
    ├── attendance                   # FR-ATT: Shift, AttendanceRecord, RegularizationRequest
    ├── leave                        # FR-LEV: LeaveType, LeaveBalance, LeaveRequest
    ├── payroll                      # FR-PAY: SalaryComponent, PayrollPeriod, PayrollRun, Payslip
    ├── audit                        # FR-AUD: AuditEvent, AuditService
    └── notification                 # FR-NOT: In-app workflow notification entity
```

---

## 🚀 Getting Started

### 1. Prerequisites
- **Java 21 LTS** installed (`java -version`)
- **Docker Desktop** (for containerized PostgreSQL and pgAdmin)

### 2. Start PostgreSQL & pgAdmin
Run the included Docker Compose stack:
```bash
docker compose up -d postgres pgadmin
```
- **PostgreSQL:** Port `5432` (db: `ga_hrms`, user: `postgres`, password: `postgres`)
- **pgAdmin 4:** Port `5050` (email: `admin@gahrms.com`, password: `admin`)

### 3. Build & Run Application
Use the included Maven wrapper:

**On Windows (PowerShell / CMD):**
```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run
```

**On Linux / macOS:**
```bash
./mvnw clean compile
./mvnw spring-boot:run
```

The application runs on `http://localhost:8080`.

---

## 📖 API Documentation & Endpoints

- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI 3 JSON Spec:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Actuator Health:** [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

### Authentication Endpoints (FR-IAM)
- `POST /api/v1/auth/login` — Login with credentials (email & password)
- `POST /api/v1/auth/refresh` — Refresh access token using refresh token
- `POST /api/v1/auth/logout` — Revoke session / refresh token
- `GET /api/v1/auth/me` — Retrieve current authenticated user profile

### Bootstrap Super Admin Credentials
- **Email:** `admin@gahrms.com`
- **Password:** `Admin@123456`
- **Role:** `ROLE_SUPER_ADMIN`

---

## 🔒 Security & Roles (RBAC)
- `ROLE_SUPER_ADMIN`
- `ROLE_HR_ADMIN`
- `ROLE_MANAGER`
- `ROLE_PAYROLL_ADMIN`
- `ROLE_EMPLOYEE`
- `ROLE_AUDITOR_READONLY`
- `ROLE_MANAGEMENT_VIEWER`
