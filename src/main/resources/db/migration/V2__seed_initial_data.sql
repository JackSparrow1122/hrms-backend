-- =============================================================================
-- GA-HRMS Baseline Seed Data V2
-- Initial Roles, Company Setup, Admin User, Sample Master Data
-- =============================================================================

-- 1. Standard Roles
INSERT INTO roles (name, description) VALUES
('ROLE_SUPER_ADMIN', 'System-wide governance, infrastructure settings, role delegation'),
('ROLE_HR_ADMIN', 'Employee lifecycle management, company departments, attendance/leave governance'),
('ROLE_MANAGER', 'Team visibility, attendance regularization and leave approval workflows'),
('ROLE_PAYROLL_ADMIN', 'Salary components, structures, payroll periods and run finalization'),
('ROLE_EMPLOYEE', 'Self-service workspace (profile, attendance punch, leave requests, payslips)'),
('ROLE_AUDITOR_READONLY', 'Compliance inspection, system audit logs and reporting review'),
('ROLE_MANAGEMENT_VIEWER', 'Executive workforce KPIs and aggregated departmental analytics');

-- 2. Baseline Company
INSERT INTO companies (name, code, registration_number, tax_id, timezone, currency)
VALUES ('Global Automations HRMS Inc.', 'GA-CORP', 'U72900MH2026PTC123456', '27AABCU9603R1ZM', 'Asia/Kolkata', 'INR');

-- 3. Baseline Branches
INSERT INTO branches (company_id, name, code, city, state, country, is_headquarters) VALUES
(1, 'Mumbai HQ & Corporate Office', 'B-MUM-01', 'Mumbai', 'Maharashtra', 'India', TRUE),
(1, 'Bangalore Tech Center', 'B-BLR-01', 'Bengaluru', 'Karnataka', 'India', FALSE);

-- 4. Baseline Departments
INSERT INTO departments (company_id, name, code, is_active) VALUES
(1, 'Executive Leadership', 'DEP-EXEC', TRUE),
(1, 'Human Resources', 'DEP-HR', TRUE),
(1, 'Software Engineering', 'DEP-ENG', TRUE),
(1, 'Finance & Payroll', 'DEP-FIN', TRUE),
(1, 'Operations & Facilities', 'DEP-OPS', TRUE);

-- 5. Baseline Designations
INSERT INTO designations (company_id, name, code, level, is_active) VALUES
(1, 'Chief Executive Officer', 'DES-CEO', 10, TRUE),
(1, 'Chief Technology Officer', 'DES-CTO', 9, TRUE),
(1, 'Head of Human Resources', 'DES-HHR', 8, TRUE),
(1, 'Senior HR Specialist', 'DES-SHR', 6, TRUE),
(1, 'Principal Software Engineer', 'DES-PSE', 7, TRUE),
(1, 'Senior Software Engineer', 'DES-SSE', 5, TRUE),
(1, 'Software Engineer', 'DES-SWE', 4, TRUE),
(1, 'Payroll & Compliance Specialist', 'DES-PAY', 5, TRUE);

-- 6. Baseline Shifts
INSERT INTO shifts (company_id, name, code, start_time, end_time, break_duration_minutes, is_overnight, is_active) VALUES
(1, 'Standard Day Shift', 'SH-DAY', '09:30:00', '18:30:00', 60, FALSE, TRUE),
(1, 'Early Shift', 'SH-EARLY', '07:00:00', '16:00:00', 60, FALSE, TRUE),
(1, 'Late Shift', 'SH-LATE', '13:00:00', '22:00:00', 60, FALSE, TRUE);

-- 7. Baseline Leave Types
INSERT INTO leave_types (company_id, name, code, annual_allowance, is_paid, requires_document, is_active) VALUES
(1, 'Casual Leave', 'CL', 12.00, TRUE, FALSE, TRUE),
(1, 'Sick Leave', 'SL', 10.00, TRUE, TRUE, TRUE),
(1, 'Earned Leave', 'EL', 15.00, TRUE, FALSE, TRUE),
(1, 'Compensatory Off', 'COMP-OFF', 0.00, TRUE, FALSE, TRUE),
(1, 'Maternity Leave', 'ML', 182.00, TRUE, TRUE, TRUE),
(1, 'Paternity Leave', 'PL', 15.00, TRUE, FALSE, TRUE);

-- 8. Super Admin Bootstrap User
-- Password: "Admin@123456" (BCrypt hashed)
INSERT INTO users (email, password_hash, first_name, last_name, phone_number, is_active, is_email_verified)
VALUES (
    'admin@gahrms.com',
    '$2a$12$Nq9v7Jx5GjCiqZ6V45eGvuXv2c4L9i9039R8P.Ww20l/bWv5L2Yhe',
    'System',
    'Administrator',
    '+91-9876543210',
    TRUE,
    TRUE
);

-- Assign SUPER_ADMIN role to bootstrap admin
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'admin@gahrms.com' AND r.name = 'ROLE_SUPER_ADMIN';
