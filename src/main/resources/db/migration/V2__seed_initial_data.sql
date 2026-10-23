-- =============================================================================
-- GA-HRMS Baseline Seed Data V2
-- Initial Roles and Bootstrap Super Admin User ONLY
-- =============================================================================

-- 1. Standard Platform Roles
INSERT INTO roles (name, description) VALUES
('ROLE_SUPER_ADMIN', 'Platform Owner: System-wide governance, infrastructure settings, role delegation'),
('ROLE_TENANT_ADMIN', 'Organization Head HR: Controls company branches, departments, and delegates Branch HR Admins'),
('ROLE_HR_ADMIN', 'Branch HR Admin: Manages branch workforce, departments, and operations'),
('ROLE_MANAGER', 'Department Manager: Team visibility, attendance regularization, and approval workflows'),
('ROLE_TEAM_LEAD', 'Team Lead: Operational team supervision and task delegation'),
('ROLE_PAYROLL_ADMIN', 'Payroll Specialist: Salary components, structures, payroll periods and run finalization'),
('ROLE_EMPLOYEE', 'Workforce ESS: Self-service workspace (profile, punch, leave requests, payslips)'),
('ROLE_AUDITOR_READONLY', 'Auditor: Compliance inspection, system audit logs and reporting review'),
('ROLE_MANAGEMENT_VIEWER', 'Executive Viewer: Workforce KPIs and aggregated analytics')
ON CONFLICT (name) DO NOTHING;

-- 2. Super Admin Bootstrap User
-- Password: "Admin@123456" (BCrypt hashed)
INSERT INTO users (email, password_hash, first_name, last_name, phone_number, is_active, is_email_verified)
VALUES (
    'admin@gahrms.com',
    '$2a$10$QWq.bVpUoD.YlM6J1UHi9exXl1EifcBuo/rG0EjVCPSMPNFjggsvG',
    'System',
    'Administrator',
    '+91-9876543210',
    TRUE,
    TRUE
)
ON CONFLICT (email) DO NOTHING;

-- 3. Assign SUPER_ADMIN role to bootstrap admin
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'admin@gahrms.com' AND r.name = 'ROLE_SUPER_ADMIN'
ON CONFLICT DO NOTHING;
