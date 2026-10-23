-- =============================================================================
-- GA-HRMS Multi-Tenant Architecture & Enterprise RBAC Scopes - Migration V3
-- Supports Platform-Level & Organization-Level Roles, Tenant Data Isolation
-- =============================================================================

-- 1. Ensure Roles Exist
INSERT INTO roles (name, description) VALUES
('ROLE_TENANT_ADMIN', 'Organization Head HR: Controls company branches, departments, and delegates Branch HR Admins'),
('ROLE_TEAM_LEAD', 'Team Lead: Operational team supervision, attendance verification, and review of team requests')
ON CONFLICT (name) DO NOTHING;

-- 2. Enhance Companies Table to Support Multi-Tenant Lifecycle
ALTER TABLE companies ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'ACTIVE' NOT NULL;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS subscription_plan VARCHAR(50) DEFAULT 'ENTERPRISE';
ALTER TABLE companies ADD COLUMN IF NOT EXISTS max_employees INT DEFAULT 500;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS max_branches INT DEFAULT 5;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS contact_email VARCHAR(150);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(50);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS domain VARCHAR(100);

-- 3. Enhance Users Table to Anchor Tenant & Data Access Scope
ALTER TABLE users ADD COLUMN IF NOT EXISTS company_id BIGINT REFERENCES companies(id) ON DELETE SET NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS scope_type VARCHAR(30) DEFAULT 'ORGANIZATION' NOT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS scope_branch_id BIGINT REFERENCES branches(id) ON DELETE SET NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS scope_department_id BIGINT REFERENCES departments(id) ON DELETE SET NULL;

-- 4. Super Admin: Platform Scope
UPDATE users
SET scope_type = 'PLATFORM', company_id = NULL
WHERE email = 'admin@gahrms.com';
