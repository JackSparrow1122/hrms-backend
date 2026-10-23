-- =============================================================================
-- GA-HRMS Database Schema - Baseline Migration V1
-- Compliant with HRMS Production PRD v1.1
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- -----------------------------------------------------------------------------
-- 1. Identity & Access Management (FR-IAM)
-- -----------------------------------------------------------------------------
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    module VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    is_email_verified BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0 NOT NULL
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- -----------------------------------------------------------------------------
-- 2. Organization Structure (FR-ORG)
-- -----------------------------------------------------------------------------
CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    registration_number VARCHAR(100),
    tax_id VARCHAR(100),
    timezone VARCHAR(50) DEFAULT 'Asia/Kolkata' NOT NULL,
    currency VARCHAR(10) DEFAULT 'INR' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE branches (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) NOT NULL,
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100) DEFAULT 'India',
    is_headquarters BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_branches_code UNIQUE (company_id, code)
);

CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    parent_department_id BIGINT REFERENCES departments(id) ON DELETE SET NULL,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_dept_code UNIQUE (company_id, code)
);

CREATE TABLE designations (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    department_id BIGINT REFERENCES departments(id) ON DELETE SET NULL,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) NOT NULL,
    level INTEGER DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_desig_code UNIQUE (company_id, code)
);

-- -----------------------------------------------------------------------------
-- 3. Employee Master & Lifecycle (FR-EMP)
-- -----------------------------------------------------------------------------
CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    date_of_birth DATE,
    gender VARCHAR(20),
    joining_date DATE NOT NULL,
    confirmation_date DATE,
    status VARCHAR(30) DEFAULT 'ACTIVE' NOT NULL, -- ACTIVE, PROBATION, NOTICE_PERIOD, SUSPENDED, SEPARATED
    company_id BIGINT NOT NULL REFERENCES companies(id),
    branch_id BIGINT REFERENCES branches(id),
    department_id BIGINT REFERENCES departments(id),
    designation_id BIGINT REFERENCES designations(id),
    manager_id BIGINT REFERENCES employees(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0 NOT NULL
);

-- Circular FK for users.employee_id
ALTER TABLE users ADD CONSTRAINT fk_users_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE SET NULL;

CREATE TABLE employee_documents (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT,
    checksum VARCHAR(64),
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- -----------------------------------------------------------------------------
-- 4. Attendance & Shift Management (FR-ATT)
-- -----------------------------------------------------------------------------
CREATE TABLE shifts (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_duration_minutes INTEGER DEFAULT 60 NOT NULL,
    is_overnight BOOLEAN DEFAULT FALSE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_shift_code UNIQUE (company_id, code)
);

CREATE TABLE attendance_records (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    shift_id BIGINT REFERENCES shifts(id),
    date DATE NOT NULL,
    punch_in TIMESTAMP WITH TIME ZONE,
    punch_out TIMESTAMP WITH TIME ZONE,
    worked_minutes INTEGER DEFAULT 0 NOT NULL,
    status VARCHAR(30) DEFAULT 'PRESENT' NOT NULL, -- PRESENT, ABSENT, HALF_DAY, ON_LEAVE, HOLIDAY, WEEKLY_OFF
    source VARCHAR(30) DEFAULT 'WEB' NOT NULL,    -- WEB, MOBILE, BIOMETRIC, MANUAL, REGULARIZED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_emp_attendance_date UNIQUE (employee_id, date)
);

CREATE TABLE regularization_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    attendance_record_id BIGINT REFERENCES attendance_records(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    proposed_punch_in TIMESTAMP WITH TIME ZONE NOT NULL,
    proposed_punch_out TIMESTAMP WITH TIME ZONE NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING' NOT NULL, -- PENDING, APPROVED, REJECTED, CANCELLED
    approver_id BIGINT REFERENCES employees(id),
    reviewer_remarks VARCHAR(500),
    decided_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- -----------------------------------------------------------------------------
-- 5. Leave Management (FR-LEV)
-- -----------------------------------------------------------------------------
CREATE TABLE leave_types (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(30) NOT NULL,
    annual_allowance DECIMAL(5,2) NOT NULL,
    is_paid BOOLEAN DEFAULT TRUE NOT NULL,
    requires_document BOOLEAN DEFAULT FALSE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_leave_type_code UNIQUE (company_id, code)
);

CREATE TABLE leave_balances (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    leave_type_id BIGINT NOT NULL REFERENCES leave_types(id) ON DELETE CASCADE,
    year INTEGER NOT NULL,
    total_allowance DECIMAL(5,2) DEFAULT 0.00 NOT NULL,
    used_count DECIMAL(5,2) DEFAULT 0.00 NOT NULL,
    pending_count DECIMAL(5,2) DEFAULT 0.00 NOT NULL,
    remaining_count DECIMAL(5,2) DEFAULT 0.00 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_emp_leave_year UNIQUE (employee_id, leave_type_id, year)
);

CREATE TABLE leave_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    leave_type_id BIGINT NOT NULL REFERENCES leave_types(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days DECIMAL(4,1) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING' NOT NULL, -- PENDING, APPROVED, REJECTED, CANCELLED
    approver_id BIGINT REFERENCES employees(id),
    reviewer_remarks VARCHAR(500),
    decided_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- -----------------------------------------------------------------------------
-- 6. Payroll Foundation & Payslips (FR-PAY)
-- -----------------------------------------------------------------------------
CREATE TABLE salary_components (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL, -- EARNING, DEDUCTION
    is_taxable BOOLEAN DEFAULT TRUE NOT NULL,
    is_statutory BOOLEAN DEFAULT FALSE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_salary_component UNIQUE (company_id, code)
);

CREATE TABLE salary_structures (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    effective_from DATE NOT NULL,
    effective_to DATE,
    base_salary DECIMAL(12,2) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE payroll_periods (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    pay_date DATE NOT NULL,
    status VARCHAR(30) DEFAULT 'DRAFT' NOT NULL, -- DRAFT, PROCESSING, FINALIZED, CANCELLED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE payroll_runs (
    id BIGSERIAL PRIMARY KEY,
    payroll_period_id BIGINT NOT NULL REFERENCES payroll_periods(id) ON DELETE CASCADE,
    status VARCHAR(30) DEFAULT 'DRAFT' NOT NULL, -- DRAFT, CALCULATED, REVIEWED, FINALIZED, CANCELLED
    total_employees INTEGER DEFAULT 0 NOT NULL,
    gross_payout DECIMAL(14,2) DEFAULT 0.00 NOT NULL,
    total_deductions DECIMAL(14,2) DEFAULT 0.00 NOT NULL,
    net_payout DECIMAL(14,2) DEFAULT 0.00 NOT NULL,
    calculated_at TIMESTAMP WITH TIME ZONE,
    approved_at TIMESTAMP WITH TIME ZONE,
    finalized_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE payslips (
    id BIGSERIAL PRIMARY KEY,
    payroll_run_id BIGINT NOT NULL REFERENCES payroll_runs(id) ON DELETE CASCADE,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    gross_earnings DECIMAL(12,2) NOT NULL,
    total_deductions DECIMAL(12,2) NOT NULL,
    net_pay DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) DEFAULT 'GENERATED' NOT NULL,
    file_path VARCHAR(500),
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- -----------------------------------------------------------------------------
-- 7. Audit Events & Notifications (FR-AUD, FR-NOT)
-- -----------------------------------------------------------------------------
CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT,
    actor_email VARCHAR(150),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100),
    before_state TEXT,
    after_state TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(300),
    status VARCHAR(30) NOT NULL,
    correlation_id VARCHAR(100),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) DEFAULT 'INFO' NOT NULL,
    target_url VARCHAR(300),
    is_read BOOLEAN DEFAULT FALSE NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_employees_code ON employees(employee_code);
CREATE INDEX idx_employees_dept ON employees(department_id);
CREATE INDEX idx_attendance_emp_date ON attendance_records(employee_id, date);
CREATE INDEX idx_leave_requests_emp ON leave_requests(employee_id, status);
CREATE INDEX idx_audit_timestamp ON audit_events(timestamp);
CREATE INDEX idx_notifications_recipient ON notifications(recipient_id, is_read);
