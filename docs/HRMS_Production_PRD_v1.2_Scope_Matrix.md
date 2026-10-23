PRODUCT REQUIREMENTS
DOCUMENT

Human Resource Management System

Production-grade web platform | Business, functional, technical and operational requirements


| Document Control | Details |
| --- | --- |
| Version | 1.2 — Multi-tenant architecture baseline, configurable business rules, policy versioning, and scope alignment added |
| Status | Approved Architecture Baseline & Functional Specification |
| Prepared by | IT Department / Product Owner / Architecture Review Board |
| Date | 15 October 2026 |
| Primary audience | Management, HR, Finance, IT, Engineering, QA and Operations |
| Product direction | Multi-Tenant Enterprise HRMS SaaS Platform — shared application with strict tenant isolation, configurable policy engine, and modular core |
| Proposed stack | React + Vite; Spring Boot / Java 21; PostgreSQL; REST APIs; JWT-based authentication (subject to architecture approval) |
| Deployment decision | AWS / Airtel Cloud / other approved hosting — pending technical and commercial evaluation |


Confidentiality: This document contains internal product planning information. It is intended for authorized stakeholders and should be reviewed before external distribution.


# Document Navigation

- 1. Executive Summary
- 2. Product Vision, Goals and Success Metrics
- 3. Business Context and Problem Statement
- 4. Users, Personas and Role Model
- 5. Product Scope and Release Boundaries
- 6. Information Architecture and Navigation
- 7. Functional Requirements by Module
- 8. End-to-End Business Workflows
- 9. Business Rules and Validation Catalogue
- 10. Data Model and Entity Relationships
- 11. API and Integration Requirements
- 12. Security, Privacy and Compliance
- 13. Non-Functional Requirements
- 14. Production Architecture and Technology
- 15. Deployment, Environments and Operations
- 16. Testing Strategy and Acceptance Criteria
- 17. Delivery Roadmap and Governance
- 18. Risks, Dependencies and Mitigations
- 19. Assumptions, Open Decisions and Change Control
- 20. Acceptance, Handover and Sign-Off
- Appendix A — Requirement ID Catalogue
- Appendix B — Suggested API Resource Catalogue
- Appendix C — Production Readiness Checklist
- Appendix D — Vendor Feature Scope & Release Classification Matrix
How to use this PRD: Treat confirmed requirements as the delivery baseline. Items marked TBD, proposed or subject to approval are decisions, not commitments. Requirement IDs should be retained in design, development, QA and release tracking.


# 1. Executive Summary

The Human Resource Management System (HRMS) is proposed as a production-grade web application to centralize employee information and standardize core HR workflows across employee lifecycle management, attendance, leave, payroll preparation, employee self-service, reporting and administration.

The product is intended to replace fragmented records and inconsistent workflows with a controlled, auditable application that provides role-appropriate access, consistent data relationships and clear workflow ownership. It must be designed for real operational use, including security, data integrity, recovery, monitoring, testing and maintainability—not merely for visual demonstration.

The initial release should prioritize the core HR foundation and high-frequency workflows. Payroll features must be scoped carefully: salary structures and payroll processing can be implemented, but statutory calculations and filings require validated company rules, current legal requirements and Finance sign-off before production use.


## 1.1 Product Summary


| Dimension | Product direction |
| --- | --- |
| Product | Multi-tenant cloud HRMS SaaS platform for employee and HR operations across multiple customer organizations |
| Primary users | Employees, HR administrators, managers, payroll/Finance users, system administrators |
| Frontend | React + Vite; responsive, accessible enterprise UI |
| Backend | Spring Boot on Java 21; REST APIs; layered modular architecture |
| Database | PostgreSQL with tenant scoping, versioned migrations, relational constraints, composite indexes and transactional operations |
| Authentication | Secure login and session/token lifecycle; JWT is a proposed option subject to security design |
| Authorization | Server-enforced tenant-isolated role-based and record-level access control with configurable permissions |
| Deployment | Cloud-hosted production environment; provider selected after cost, region, security and operational review |
| Product delivery | Phased releases with acceptance criteria, QA gates and documented approvals |



## 1.2 Executive Outcomes

- One authoritative employee record with defined ownership and lifecycle status.
- Traceable attendance correction and leave approval workflows.
- Consistent salary structure and payroll-period processing with reconciled totals.
- Role-appropriate employee self-service and manager views.
- Operational dashboards and reports based on validated source data.
- Security, auditability, backup, recovery and deployment practices appropriate for production.
- An extensible foundation for future integrations without committing to unapproved scope.

# 2. Product Vision, Goals and Success Metrics


## 2.1 Vision

Deliver a dependable, easy-to-use HR platform that gives employees access to relevant HR services, gives HR a controlled operational workspace, and gives management reliable visibility into workforce processes.


## 2.2 Product Goals

- Centralize employee master data and reduce avoidable duplicate records.
- Standardize request submission, review, approval, rejection and history.
- Make policy-driven rules explicit, testable and configurable where appropriate.
- Provide reliable reports with consistent definitions and traceable filters.
- Protect confidential employee and payroll information through least-privilege access.
- Support maintainable, observable releases and recoverable production operations.

## 2.3 Proposed Success Metrics


| Metric | Definition | Target / baseline approach |
| --- | --- | --- |
| Employee record completeness | Percentage of active employee records meeting approved mandatory-field rules | Set target after HR validates mandatory fields |
| Workflow completion | Percentage of submitted requests reaching a valid terminal state | Establish baseline during pilot |
| Approval turnaround | Elapsed time from request submission to final decision | Set SLA by request type and approver |
| Data reconciliation | Difference between source records and dashboard/report totals | Zero unexplained differences in agreed reports |
| Payroll reconciliation | Difference between component totals and payroll summary totals | Zero arithmetic discrepancies in tested scenarios |
| Availability | Production service availability measured over an agreed period | SLO to be approved after hosting architecture |
| Recovery readiness | Successful completion of scheduled backup and restore tests | Restore test required before production sign-off |
| Security defects | Open critical/high security findings at release gate | No unresolved critical/high findings without formal risk acceptance |
| User adoption | Active usage by intended users during pilot | Baseline and target agreed with HR/management |


Targets must not be invented to make the document look stronger. Product and service-level targets should be approved once operational context, baseline data and hosting design are known.


# 3. Business Context and Problem Statement


## 3.1 Problem Statement

HR operations can become difficult to coordinate when employee details, attendance information, leave requests, salary inputs and approvals are maintained in disconnected tools or manual records. This can cause repeated data entry, inconsistent status visibility, unclear ownership and avoidable reconciliation work.


## 3.2 Business Needs

- Maintain a reliable employee master record with clear lifecycle history.
- Provide transparent approval ownership and request status.
- Keep historical records available when employees or organizational structures change.
- Make payroll inputs and calculations reviewable before a run is finalized.
- Produce consistent reports from the same underlying data and business definitions.
- Protect personal, employment and compensation information.
- Support controlled changes to policies, roles and application configuration.

## 3.3 Expected Benefits and Measurement

Expected benefits include improved consistency, reduced manual follow-up, better traceability and more reliable management reporting. These are hypotheses to validate during pilot usage; the project should measure actual effort, turnaround time, error rates and adoption rather than claim unverified savings.


# 4. Users, Personas and Role Model


| Persona | Primary needs | Typical permissions |
| --- | --- | --- |
| Employee | Own profile, attendance, leave balance, requests, payslips and notifications | Self-service access to own records; submit and track requests |
| HR Administrator | Employee lifecycle, organization data, policies, attendance exceptions, leave administration and HR reports | Manage HR records and workflows within assigned scope |
| Department Manager | Team visibility and assigned approval queues | View permitted team records; approve/reject assigned requests |
| Payroll / Finance User | Salary structures, payroll inputs, payroll runs, reconciliations and payslips | Access compensation and payroll features based on approved scope |
| System Administrator | Users, roles, configuration, integration settings and operational support | Administrative controls; sensitive data access should be restricted by need |
| Auditor / Read-only Reviewer | Traceable records, approved reports and audit events | Read-only, time-bounded access if required |
| Management Viewer | Workforce KPIs and approved summary reports | Aggregated reporting; no default access to unnecessary personal details |



## 4.1 Authorization Principles

- All access decisions must be enforced by the backend; hiding a button in the frontend is not authorization.
- Apply least privilege and deny access by default.
- Separate system administration from payroll and HR business access where practicable.
- Restrict employee self-service to the authenticated employee's own records.
- Scope manager access to assigned teams or reporting relationships.
- Record security-sensitive administrative actions in an audit trail.
- Use a formal process to grant, review, change and revoke access.

## 4.2 Proposed Roles

Initial role names are proposed and must be confirmed with management and HR: SUPER_ADMIN, HR_ADMIN, MANAGER, PAYROLL_ADMIN, EMPLOYEE, AUDITOR_READONLY and MANAGEMENT_VIEWER. Permissions should be granular enough to avoid giving every administrator unrestricted access to all confidential records.


# 5. Product Scope and Release Boundaries


## 5.1 Proposed Release 1 — Core Production HRMS


| Module | Release 1 scope | Priority |
| --- | --- | --- |
| Identity and access | Login/logout, password or approved identity flow, role assignment, access enforcement, account lifecycle | P0 |
| Tenant architecture & Organization setup | Tenant boundary and context enforcement, tenant-specific settings, multi-branch/locations, departments, designations, reporting relationships, holidays, and tenant-level feature entitlements. | P0 |
| Employee lifecycle | Employee profiles, employment details, onboarding checklist, status changes, document metadata | P0 |
| Attendance | Attendance records/import-ready structure, shifts, attendance exceptions, regularization requests | P0 |
| Leave | Leave types, policy rules, balances, requests, approval workflow, history | P0 |
| Payroll foundation | Salary components/structures, payroll periods, inputs, calculation engine boundary, draft/review/finalize states | P0 |
| Payslips | Controlled payslip generation and employee access after approved payroll finalization | P0 |
| Reports | Employee, attendance, leave and payroll summary reports with role-based access | P0 |
| Notifications | In-app notifications for supported workflow events | P1 |
| Audit and administration | Audit events, settings, role administration and operational configuration | P0 |
| Deployment operations | Environment separation, CI/CD, backups, monitoring, logs and restore procedures | P0 |
| Policy & Workflow Engine | Versioned policy definitions (attendance late/grace/regularization, leave accrual/carryover), policy assignments by branch/dept/group, and configurable multi-step approval workflows with delegation & escalation. | P0 |



## 5.2 Potential Later Releases

- Biometric/attendance-device integration after device and vendor selection.
- Email/SMS/WhatsApp notifications after provider, cost and consent review.
- Accounting/ERP integration after interface and ownership are agreed.
- Advanced analytics, workforce planning and configurable dashboards.
- Mobile application or progressive web application enhancements.
- Advanced recruitment, performance management, learning, expenses or asset management only if explicitly approved as product scope.

## 5.3 Out of Scope Unless Approved

- Unapproved third-party integrations or paid provider commitments.
- Automatic legal/tax filing without validated rules and authorized integration.
- Bank payment execution or direct disbursement in the initial release unless explicitly required and approved.
- Features unrelated to HRMS core objectives that materially increase release risk.
- Treating prototype-only shortcuts as acceptable production security controls.

# 6. Information Architecture and Navigation


| Navigation group | Primary pages |
| --- | --- |
| Home | Role-specific dashboard, assigned tasks, alerts and recent activity |
| People | Employee directory, employee profile, onboarding, departments, designations, branches, reporting structure, documents |
| Time & Attendance | Attendance register, daily detail, shift roster, holidays, regularization queue, imports and exceptions |
| Leave | Leave overview, policy configuration, balances, requests, approval queue and history |
| Payroll | Salary components, salary structures, payroll periods, payroll runs, exceptions, payslips and payroll reports |
| Reports | Employee, attendance, leave, payroll and audit reports permitted for the user's role |
| Administration | Users, roles, permissions, company settings, integrations, notification settings and audit logs |
| My Workspace | My profile, my attendance, my leave, my requests, my payslips and personal notifications |


The UI should use a consistent design system and original visual identity. The previously discussed palette—purple #AA00EA, deep purple #310052, cyan #A3FAFF and light neutral backgrounds—may be retained as a design direction, with accessible contrast and restrained accent usage.


# 7. Functional Requirements by Module

Priority: P0 = mandatory for the agreed first production release; P1 = important but may follow the core release; P2 = future candidate. Each requirement must be linked to implementation tickets and test cases.


## 7.1 Identity, Authentication and Access


| ID | Requirement | Priority | Acceptance summary |
| --- | --- | --- | --- |
| FR-IAM-001 | Login and logout | P0 | Users can authenticate through the approved identity mechanism; logout invalidates the applicable session/token state. |
| FR-IAM-002 | Account lifecycle | P0 | Authorized administrators can activate, deactivate and review user accounts; deactivated accounts cannot continue to access protected APIs. |
| FR-IAM-003 | Role and permission administration | P0 | Authorized administrators assign approved roles and granular permissions; changes take effect according to documented token/session rules. |
| FR-IAM-004 | Server-side authorization | P0 | Every protected endpoint validates authentication, role and record scope; direct API calls cannot bypass UI restrictions. |
| FR-IAM-005 | Credential recovery and protection | P0 | If local credentials are used, implement secure password hashing, recovery workflow, rate limiting and safe error messages; exact policy requires approval. |
| FR-IAM-006 | Session and token lifecycle | P0 | Access token lifetime, refresh/re-authentication, revocation strategy and storage model are documented and security-reviewed. |



## 7.2 Organization and Employee Lifecycle


| ID | Requirement | Priority | Acceptance summary |
| --- | --- | --- | --- |
| FR-ORG-001 | Company and branch setup | P0 | Maintain approved company/branch/location records and effective dates where needed. |
| FR-ORG-002 | Departments and designations | P0 | Create, edit, deactivate and assign organization entities; prevent invalid references. |
| FR-EMP-001 | Employee directory | P0 | Search, filter, sort and paginate employee records according to permission scope. |
| FR-EMP-002 | Employee profile | P0 | Maintain identity, contact, employment, reporting, department, designation and lifecycle details with field-level validation. |
| FR-EMP-003 | Unique employee identifier | P0 | Employee identifier is unique within the defined organization scope; duplicate attempts return a clear validation error. |
| FR-EMP-004 | Onboarding checklist | P1 | Track required onboarding steps, owners, due dates and completion state. |
| FR-EMP-005 | Employee status and separation | P0 | Support active, inactive, separated and other approved statuses; preserve historical transactions. |
| FR-EMP-006 | Employee documents | P1 | Store document metadata and use secure object/file storage for actual files; access and retention rules are enforced. |
| FR-EMP-007 | Employee import | P1 | Provide validated bulk import with preview, row-level errors, duplicate detection and an import result report. |



## 7.3 Attendance and Shift Management


| ID | Requirement | Priority | Acceptance summary |
| --- | --- | --- | --- |
| FR-ATT-001 | Attendance register | P0 | View attendance by date, employee, department and status within the user's scope. |
| FR-ATT-002 | Attendance source and provenance | P0 | Each attendance record identifies whether it originated from manual entry, import, integration or approved correction. |
| FR-ATT-003 | Shift configuration | P0 | Configure shift start/end times, break rules, overnight handling and effective dates according to approved policy. |
| FR-ATT-004 | Attendance calculation | P0 | Calculate worked duration and status using approved rules; handle overnight shifts, missing punches and invalid sequences. |
| FR-ATT-005 | Regularization workflow | P0 | Employee submits correction request; reviewer decides; original value, proposed value, reason, decision and timestamps remain traceable. |
| FR-ATT-006 | Bulk attendance import | P1 | Import supported CSV/XLSX format after template approval; validate rows and provide retryable error reporting. |
| FR-ATT-007 | Attendance exceptions | P0 | Flag missing punches, duplicate entries, unassigned shifts and other defined exceptions for authorized review. |
| FR-ATT-008 | Holidays and weekly offs | P0 | Maintain holiday calendars and weekly-off rules by approved scope and effective date. |



## 7.4 Leave Management


| ID | Requirement | Priority | Acceptance summary |
| --- | --- | --- | --- |
| FR-LEV-001 | Leave type and policy setup | P0 | Configure leave types, eligibility, entitlement, accrual and other approved policy parameters. |
| FR-LEV-002 | Leave application | P0 | Employee submits a date-range request with leave type and reason; server validates dates and eligibility. |
| FR-LEV-003 | Balance calculation | P0 | Available, accrued, used, pending and adjusted balances follow documented formulas and are consistent across screens. |
| FR-LEV-004 | Approval routing | P0 | Requests route to the configured manager/HR approver; missing or inactive approvers create a visible exception. |
| FR-LEV-005 | Approval/rejection | P0 | Only authorized approvers can act; rejection requires a reason; terminal requests cannot be decided again. |
| FR-LEV-006 | Cancellation and amendment | P1 | Define rules for pending and approved leave cancellation/amendment, including approval and balance reversal. |
| FR-LEV-007 | Leave history and reports | P0 | Employees and authorized reviewers can see status history and period summaries within permission scope. |



## 7.5 Payroll and Payslips


| ID | Requirement | Priority | Acceptance summary |
| --- | --- | --- | --- |
| FR-PAY-001 | Payroll configuration | P0 | Maintain approved pay frequency, payroll periods, components and rounding rules. |
| FR-PAY-002 | Salary structures | P0 | Maintain effective-dated earnings and deductions; changes do not silently alter finalized historical payroll. |
| FR-PAY-003 | Payroll input collection | P0 | Collect approved attendance, leave, variable pay, deductions and other inputs with provenance and review status. |
| FR-PAY-004 | Calculation engine | P0 | Calculate gross earnings, deductions and net pay using versioned, testable rules approved by Finance. |
| FR-PAY-005 | Payroll run workflow | P0 | Support draft, calculated, under review, approved/finalized and cancelled/reversed states as approved by Finance. |
| FR-PAY-006 | Exception handling | P0 | Flag missing salary data, negative/invalid values, missing inputs and reconciliation issues before finalization. |
| FR-PAY-007 | Payroll reconciliation | P0 | Component totals reconcile with employee-level and run-level totals; rounding rules are consistent. |
| FR-PAY-008 | Payslip generation | P0 | Generate a controlled payslip for finalized payroll only, with authorized access and correct employee/period mapping. |
| FR-PAY-009 | Payroll correction | P1 | Define controlled post-finalization adjustment or reversal with reason, approval and audit history. |
| FR-PAY-010 | Statutory rules | P0 | Applicable statutory calculations are implemented only after requirements, effective dates, tests and Finance/legal sign-off are approved. |



## 7.6 Employee Self-Service


| ID | Requirement | Priority | Acceptance summary |
| --- | --- | --- | --- |
| FR-ESS-001 | Own profile | P0 | Employee can view permitted personal/employment details and request changes where policy allows. |
| FR-ESS-002 | Own attendance and leave | P0 | Employee can view only their authorized attendance and leave information. |
| FR-ESS-003 | Own payslips | P0 | Employee can access only payslips associated with their authenticated identity and finalized payroll periods. |
| FR-ESS-004 | Request tracking | P0 | Employee can see request status, submission time, decision and permitted reviewer comments. |



## 7.7 Reports, Dashboards and Notifications


| ID | Requirement | Priority | Acceptance summary |
| --- | --- | --- | --- |
| FR-REP-001 | Dashboard metrics | P0 | Metrics are calculated from authoritative records and have documented definitions and filters. |
| FR-REP-002 | Employee report | P0 | Authorized users can filter employee data and export approved fields. |
| FR-REP-003 | Attendance report | P0 | Period totals reconcile with underlying attendance records and approved status rules. |
| FR-REP-004 | Leave report | P0 | Report includes approved, pending, rejected and cancelled states as defined by filters. |
| FR-REP-005 | Payroll report | P0 | Compensation details are restricted to authorized roles and totals reconcile with payroll runs. |
| FR-REP-006 | Export controls | P0 | Exports respect the same row/field permissions as the UI and are logged where sensitive. |
| FR-NOT-001 | In-app notifications | P1 | Supported workflow events create notifications for the correct recipient and link to an authorized destination. |
| FR-NOT-002 | Notification preferences | P2 | Users can configure supported preferences where approved. |



## 7.8 Audit, Administration and Support


| ID | Requirement | Priority | Acceptance summary |
| --- | --- | --- | --- |
| FR-AUD-001 | Business audit trail | P0 | Record actor, action, target, timestamp, outcome and relevant before/after values for defined sensitive events. |
| FR-AUD-002 | Audit access | P0 | Audit events are append-oriented, access-controlled and protected from ordinary user modification. |
| FR-ADM-001 | System configuration | P0 | Authorized administrators can manage approved settings with validation and change history. |
| FR-ADM-002 | Health and readiness endpoints | P0 | Provide appropriately protected health/readiness checks for deployment and monitoring. |
| FR-ADM-003 | Operational error handling | P0 | Return consistent API errors with correlation IDs; do not expose secrets, stack traces or sensitive data. |
| FR-ADM-004 | Support diagnostics | P1 | Authorized support users can find request IDs and operational events without unrestricted access to confidential business records. |



# 8. End-to-End Business Workflows


## 8.1 Employee Onboarding

HR creates an employee record and enters mandatory identity and employment fields.

The server validates required fields, uniqueness, organization references and permitted status transitions.

HR assigns department, designation, manager, work location and effective date.

Required onboarding checklist items and document metadata are recorded if enabled.

An authorized reviewer verifies the record where the approved process requires review.

The employee account is provisioned separately from the employee master record, using the approved identity process.

Audit history records creation and sensitive changes; reports reflect the new employee.


## 8.2 Attendance Regularization

Employee selects an eligible attendance date and submits the proposed correction and reason.

Server validates the employee's access, request eligibility, duplicate pending requests and policy constraints.

Request is routed to the assigned approver; the original attendance value remains unchanged while pending.

Approver reviews original data, proposed change and reason; approval or rejection is recorded with timestamp and identity.

On approval, the system applies the correction transactionally and links the changed record to the request.

Related summaries are recalculated or queried from the updated source; the employee can see the outcome.


## 8.3 Leave Request and Approval

Employee selects leave type and date range; system shows applicable balance and policy notes.

Server validates dates, eligibility, overlap, balance rules and any configured restrictions.

Request is stored as pending and routed to the correct approver.

Approver reviews team coverage and request details, then approves or rejects with a reason where required.

Approval and balance updates occur atomically to prevent inconsistent balances or duplicate decisions.

Cancellation, amendment, reversal and concurrent requests follow the approved leave policy.

Employee, HR and manager views display consistent status and history.


## 8.4 Payroll Processing

Payroll administrator opens a payroll period and verifies period boundaries and eligible employees.

System gathers effective-dated salary structures and approved inputs from attendance, leave and other configured sources.

Calculation engine runs the approved, versioned rules and stores a calculation snapshot for review.

Validation checks identify missing inputs, invalid components, outliers and reconciliation discrepancies.

Authorized payroll reviewer examines exceptions and approves the run according to the approval matrix.

Finalization locks the payroll snapshot from silent edits; corrections use a controlled adjustment or reversal process.

Payslips become available only after finalization and are accessible only to authorized users.

Audit records capture the run, calculation version, reviewer decisions and finalization event.


## 8.5 Employee Separation

Authorized HR user records separation type, effective date and approved checklist.

System checks outstanding workflow items and records required handover tasks.

Access revocation is coordinated with the identity/account lifecycle process.

Historical attendance, leave, payroll and audit records remain retained according to approved retention rules.

Final settlement is not assumed to be automated unless separately specified and validated.


# 9. Business Rules and Validation Catalogue


| Area | Rule ID | Rule / decision required |
| --- | --- | --- |
| Employee | BR-EMP-01 | Employee ID uniqueness scope (company-wide or tenant-wide) must be approved. |
| Employee | BR-EMP-02 | Employment dates and status transitions must be validated; historical transactions must be preserved. |
| Attendance | BR-ATT-01 | Define punch pairing, missing punches, overnight shifts, breaks, grace periods, late/early rules and rounding. |
| Attendance | BR-ATT-02 | Define attendance import format, duplicate detection, timezone and correction precedence. |
| Leave | BR-LEV-01 | Define accrual, entitlement, carry-forward, expiry, negative balance, half-day and holiday/weekend treatment. |
| Leave | BR-LEV-02 | Define overlapping requests, cancellation rules, backdated requests and approval delegation. |
| Payroll | BR-PAY-01 | Define pay period, joining/leaving proration, unpaid leave treatment and variable-pay cut-off. |
| Payroll | BR-PAY-02 | Define component formulas, rounding, effective dating and treatment of retroactive changes. |
| Payroll | BR-PAY-03 | Identify applicable statutory components, thresholds, exemptions and effective dates; validate with Finance/legal expertise. |
| Security | BR-SEC-01 | Define password/session policy or approved SSO method, MFA requirements and account recovery. |
| Approval | BR-WF-01 | Define approval chain, escalation, delegation, approver absence and segregation of duties. |
| Retention | BR-DATA-01 | Define retention and deletion rules by data category, including legal holds if applicable. |


No HR or payroll rule should be hard-coded from assumption when it materially affects salary, leave entitlement, attendance status or compliance. Rules must be approved, versioned where appropriate, and covered by test cases.


# 10. Data Model and Entity Relationships


## 10.1 Proposed Core Entities


| Entity | Purpose / key relationships |
| --- | --- |
| Tenant / Organization | Company boundary, settings, timezone, policy defaults; parent scope for organization data if multi-tenancy is approved. |
| Branch / Location | Work location, timezone and location-specific calendars. |
| Department | Organizational unit; references organization and may have a parent department. |
| Designation | Position/title catalogue with active state. |
| Employee | Employee master, employment status, reporting manager, department, designation and location. |
| User Account | Authentication identity mapped to employee where applicable; distinct from employee master. |
| Role / Permission / UserRole | Authorization model and role assignment; server-enforced. |
| Shift / Shift Assignment | Shift definitions and effective-dated assignment to employees or groups. |
| Attendance Record | Employee/date attendance values, source, status and calculation metadata. |
| Attendance Import Batch | Import metadata, file hash/reference, validation results and row-level errors. |
| Regularization Request | Requested correction, original reference, reason, decision and approver. |
| Holiday Calendar / Holiday | Holiday rules and dates by organization/location. |
| Leave Type / Leave Policy | Leave category and versioned policy parameters. |
| Leave Balance Ledger | Accrual, usage, adjustment and reversal entries for explainable balance calculation. |
| Leave Request / Approval Step | Request dates, status, decision history and approval routing. |
| Salary Component | Earning/deduction definition and calculation metadata. |
| Salary Structure / Structure Component | Effective-dated component assignments for an employee or eligible group. |
| Payroll Period / Payroll Run | Period boundaries, state, calculation version, approval and finalization metadata. |
| Payroll Input / Payroll Line | Input provenance and employee-level earnings/deductions for a run. |
| Payslip | Finalized employee-period compensation snapshot and access metadata. |
| Notification | Recipient, event, status and destination reference. |
| Audit Event | Actor, event, target, timestamp, correlation ID and permitted change metadata. |
| File Metadata | Secure file reference, owner, category, checksum, scan status and retention metadata. |
| Organization Settings | Tenant-specific preferences: timezone, currency, fiscal year, working hours and policy defaults. |
| Feature Entitlement | Module licensing and capability toggles available to each customer tenant. |
| Policy Definition / Version | Canonical business rule definitions and effective-dated policy versions (attendance grace periods, late limits, leave accruals). |
| Policy Assignment | Hierarchical policy assignment to organization, legal entity, branch, department, shift, or employee group. |
| Approval Workflow / Step | Configurable multi-step approval sequences, approver roles (manager, HR, skip-level), escalation rules and delegation. |



## 10.2 Data Design Principles

- Use stable primary keys and explicit foreign-key relationships.
- Use database constraints for uniqueness, referential integrity and required fields.
- Use transactions for approval decisions, balance updates and payroll state transitions.
- Use effective dates for policy, salary and assignment changes that affect historical calculations.
- Prefer ledger or transaction records for balances that require traceability.
- Use database migrations under source control; avoid manual, undocumented production schema changes.
- Define indexes based on real query patterns, including employee, date range, status and organization scope.
- Store timestamps consistently in UTC and render them in the configured user/organization timezone.
- Use decimal types and explicit rounding rules for monetary values; do not use floating-point types for money.
- Define retention, deletion, anonymization and export rules before production data is loaded.

## 10.3 Multi-Tenant Architecture and Configurable Business Rules

Per Architecture Review, the platform baseline is formally committed as a Multi-Tenant SaaS application. The system uses a shared application and database architecture with strict logical tenant isolation enforced at every layer: REST API request interceptors, Hibernate/JPA tenant scoping filters, background scheduling jobs, analytical reports, file storage namespaces, and audit logging. Adding a tenant_id column alone is necessary but not sufficient; tenant context must be immutable and resolved from the authenticated user token for every operation.


### The Three-Tier Configuration Model

To prevent architectural conflation, the system strictly separates three distinct configuration dimensions:

1. Feature Entitlements (Platform Level): Defines which business modules and capabilities (e.g. Attendance, Leave, Payroll, Advanced Analytics) are licensed and enabled for a given customer tenant. This is managed by system administrators and determines API route and UI navigation availability.

2. Policy Configuration (Tenant & Organizational Level): Defines versioned rules for how business logic executes (e.g., attendance grace periods, regularization submission windows, leave accrual schedules, overtime multipliers). Policies are assigned hierarchically to legal entities, branches, departments, shifts, or employee groups.

3. Roles and Permissions (User Authorization Level): Defines what individual users can view or execute within their tenant. Permissions are granular and scoped by department, branch, and team reporting structure.


### Policy Versioning and Historical Immutability Guarantee

A fundamental flaw in legacy HRMS implementations is allowing administrative edits to silently mutate historical calculations. GA-HRMS guarantees data integrity through versioned policy history:

• Effective Dating: Every policy definition (e.g., 'Standard Attendance Policy') maintains an immutable chain of policy versions with effective_from and effective_to dates.

• Historical Invariance: If a company increases its grace period from 10 minutes to 15 minutes starting on 1 November, attendance records prior to 1 November remain bound to the 10-minute policy version. Finalized attendance registers and approved payroll runs are cryptographically and transactionally locked against subsequent policy edits.

• Pending Request Evaluation: When a policy changes, the policy version specifies whether pending workflow requests follow the submission-time policy or are evaluated against the newly active version.

• Full Audit Provenance: Every policy creation, version update, and assignment change records the acting administrator, exact timestamp, IP address, and JSON diff of all modified parameters.


### Multi-Tenant Architecture Acceptance Criteria

The following criteria are mandatory for the Phase 1 production baseline and must be verified by automated test suites:

• AC-MTC-01 (Tenant Isolation): Every API request, repository query, background batch job, file retrieval, and report export must enforce the active tenant boundary. Direct query execution without tenant scoping is prevented by automated database filters. Cross-tenant access attempts return HTTP 403 Forbidden and trigger high-priority security audit events.

• AC-MTC-02 (Default Policies & Tenant Overrides): The platform provides system-default policy templates while allowing each customer tenant to configure and override policy parameters independently without code changes or database redeployments.

• AC-MTC-03 (Effective-Dated Versioning): All attendance, leave, and payroll policies support versioning with future-dated and retroactive-safe boundaries. Edits create new versions; existing finalized historical transactions remain untouched.

• AC-MTC-04 (Role-Based and Record-Level Authorization): Authorization decisions are server-enforced, combining role permissions with record-level ownership checks (e.g., employees access only their own records; managers access only their assigned reporting lines).

• AC-MTC-05 (Configurable Approval Workflows): Workflow routing supports configurable multi-step approval sequences (e.g. Employee -> Manager -> Department Head -> HR Admin), approver delegation during absences, automated escalation after configurable SLA timeouts, and segregation of duties (a user cannot approve their own requests).

• AC-MTC-06 (Tenant Feature Entitlements): Module access is dynamically controlled via tenant feature entitlements. Disabled modules are completely hidden in the UI and reject API requests with standardized entitlement error codes.

• AC-MTC-07 (Backward-Compatible Shared Migrations): All database migrations must be forward- and backward-compatible to support zero-downtime shared releases across all hosted tenants.

• AC-MTC-08 (Automated Multi-Tenant Verification): CI/CD pipeline includes integration tests simulating multiple concurrent customer tenants executing differing attendance, leave, and payroll policies simultaneously, validating zero cross-tenant contamination.


# 11. API and Integration Requirements


## 11.1 API Standards

- Use versioned REST endpoints (for example, /api/v1) with consistent resource naming.
- Validate all input on the server; frontend validation is supplementary only.
- Return consistent status codes and a standard error body containing a safe message, stable error code and correlation ID.
- Use pagination, sorting and allow-listed filtering for list endpoints.
- Define request/response schemas and document APIs with OpenAPI/Swagger.
- Use idempotency or duplicate-request protections for operations where retries could create duplicate records.
- Apply transaction boundaries to multi-step business operations.
- Never expose passwords, secrets, internal stack traces or unnecessary sensitive fields in API responses or logs.
- Protect exports and file endpoints with the same authorization rules as normal record views.
- Apply rate limits and abuse protection to authentication and other sensitive endpoints.

## 11.2 Suggested Resource Catalogue


| Resource | Example endpoint family | Typical operations |
| --- | --- | --- |
| Authentication | /api/v1/auth/* | Login, logout, refresh/re-authentication, password recovery if supported |
| Users and roles | /api/v1/users, /api/v1/roles | Administer accounts, roles and permissions |
| Employees | /api/v1/employees | List, create, retrieve, update, change status |
| Organization | /api/v1/departments, /designations, /locations | Manage organization records |
| Attendance | /api/v1/attendance | List records, review details, import or calculate according to policy |
| Regularization | /api/v1/attendance-requests | Submit, review, approve, reject and track requests |
| Leave | /api/v1/leave-types, /leave-requests, /leave-balances | Configure policies, submit requests, review and retrieve balances |
| Payroll | /api/v1/payroll-periods, /payroll-runs | Prepare, calculate, review and finalize runs |
| Payslips | /api/v1/payslips | List and retrieve authorized finalized payslips |
| Reports | /api/v1/reports/* | Generate authorized report data and exports |
| Notifications | /api/v1/notifications | List, mark read and retrieve permitted notifications |
| Audit | /api/v1/audit-events | Restricted read-only search for authorized reviewers |
| Health | /actuator/health/* or approved equivalent | Operational readiness and health checks with exposure controls |



## 11.3 Integration Boundaries

Potential integrations include attendance devices, email delivery, accounting systems, identity providers and secure document storage. Each integration requires a defined business owner, vendor/API selection, authentication model, error/retry policy, reconciliation procedure, data mapping, cost review and acceptance tests. No external provider should be treated as committed until approved.


# 12. Security, Privacy and Compliance


## 12.1 Security Requirements

- Enforce TLS for all production traffic and secure transport between trusted services where applicable.
- Store passwords only using a modern adaptive password-hashing algorithm if local authentication is selected.
- Protect access and refresh tokens according to a reviewed token lifecycle and browser threat model.
- Use least-privilege roles and record-level authorization for employee, manager and payroll data.
- Validate inputs and encode outputs; use parameterized database access and safe file-handling controls.
- Protect against common web risks including injection, broken access control, CSRF where relevant, XSS, SSRF, insecure file upload and misconfiguration.
- Keep secrets in an approved secrets manager or secure deployment configuration, never in source control.
- Encrypt database, file and backup storage using provider-supported encryption and controlled key access.
- Log security events while minimizing personal data and avoiding credentials or full sensitive payloads.
- Apply dependency scanning, patching, secure code review and vulnerability management.
- Restrict production administrative access and require stronger authentication for privileged users where feasible.
- Define incident response, access review and credential rotation procedures.

## 12.2 Privacy and Data Governance

- Collect only data needed for approved HR purposes.
- Define purpose, access, retention, correction and deletion rules for each sensitive data category.
- Provide appropriate access to employees for their own permitted information.
- Restrict payroll exports and document downloads to authorized roles.
- Use synthetic or anonymized data in development and test environments.
- Record and review access to particularly sensitive operations.
- Assess applicable Indian data protection, employment, tax and record-retention obligations with qualified internal/external advisors before production launch.
- Define how data subject requests, incidents, vendor access and data transfers will be handled where applicable.

## 12.3 Audit Events

At minimum, audit employee creation/changes/status transitions, role and permission changes, attendance corrections, leave decisions, salary structure changes, payroll calculation/finalization/reversal, sensitive exports, administrative configuration changes and relevant authentication/security events. Audit records should include actor, event type, target identifier, timestamp, outcome and correlation ID, with carefully controlled before/after values.


# 13. Non-Functional Requirements


| Category | Requirement | Acceptance approach |
| --- | --- | --- |
| Availability | Define production availability objective after hosting and support model are chosen. | Monitor uptime and incident duration; report against approved SLO. |
| Performance | Core pages and APIs should meet agreed response-time targets at expected concurrency and data volume. | Load-test representative APIs and list/report queries; approve numeric thresholds before release. |
| Scalability | Application should support growth through measured capacity planning and horizontal scaling where appropriate. | Test agreed peak workload and identify database/application bottlenecks. |
| Reliability | Failures should be handled predictably without corrupting business transactions. | Test retries, transaction rollback, duplicate requests and dependency failures. |
| Data integrity | Related records and financial calculations must remain consistent. | Constraint tests, transaction tests and reconciliation suites. |
| Security | No unresolved critical/high findings at production release without formal risk acceptance. | Threat modelling, SAST/dependency scans, access-control tests and penetration testing as appropriate. |
| Accessibility | Common workflows should support keyboard use, labels, readable contrast and clear errors. | Manual and automated accessibility checks against an agreed standard. |
| Compatibility | Support an agreed browser/device matrix. | Regression testing on approved browser versions and viewport sizes. |
| Maintainability | Use modular code, consistent standards, documentation and automated tests. | Code review, coverage of critical logic and maintainability checks. |
| Observability | Provide structured logs, metrics, traces where appropriate and alerting. | Operational dashboard and alert verification. |
| Recoverability | Backups and restores must be tested; RPO/RTO targets must be approved. | Documented restore drill and evidence before production sign-off. |
| Localization/time | Use configured locale, currency, timezone and date formats consistently. | Tests for date boundaries, DST/timezone handling where relevant, and monetary formatting. |



# 14. Production Architecture and Technology


## 14.1 Proposed Logical Architecture


| Layer | Proposed technology / responsibility |
| --- | --- |
| Web client | React + Vite; responsive UI, client-side routing, accessible components and API integration. |
| API / application | Spring Boot on Java 21; REST controllers, DTO validation, application services and centralized error handling. |
| Domain layer | Business rules for employee lifecycle, attendance, leave, approval workflows and payroll orchestration. |
| Persistence | PostgreSQL; relational schema, constraints, transactions, indexes and controlled migrations. |
| Data access | Spring Data JPA/Hibernate or another approved persistence approach; query patterns reviewed for performance. |
| Authentication | JWT or another approved mechanism; token storage, expiry, refresh and revocation designed explicitly. |
| File storage | Private object storage or secure managed file service for documents and generated payslips, subject to provider choice. |
| Notifications | In-app notification records initially; external email/SMS provider selected only after approval. |
| Operations | CI/CD, environment configuration, secret management, monitoring, logs, backups and recovery procedures. |



## 14.2 Architectural Principles

- Start with a modular monolith unless scale, team structure or integration constraints justify distributed services; avoid premature microservices.
- Keep business logic out of controllers and UI components.
- Separate API contracts from persistence entities.
- Use database transactions for workflows that update multiple related records.
- Keep payroll calculations deterministic, versioned and independently testable.
- Use database migrations as the source of truth for schema evolution.
- Separate development, staging/UAT and production environments.
- Externalize configuration and secrets; never commit production credentials.
- Use asynchronous jobs only where needed and ensure retry/idempotency behaviour is defined.
- Document architecture decisions and material trade-offs in concise Architecture Decision Records (ADRs).

## 14.3 Technology Decisions Requiring Approval


| Decision | Options / proposal | Evaluation criteria |
| --- | --- | --- |
| Cloud provider | AWS, Airtel Cloud or another approved provider | India region/data residency, service availability, security, support, cost, backups, network and team capability |
| Compute | Managed application platform, VM/container deployment or equivalent | Operational burden, scale, patching, deployment and cost |
| Database hosting | Managed PostgreSQL preferred for reduced operational burden, or self-managed where justified | Availability, backups, restore, patching, HA, monitoring and cost |
| Authentication | Approved JWT/session design or enterprise identity provider | Threat model, SSO needs, token lifecycle, MFA and supportability |
| File storage | Private object storage or approved secure file service | Encryption, access control, retention, signed URL policy, malware scanning and cost |
| Email delivery | Approved SMTP/API provider if notifications require email | Deliverability, domain setup, cost, logs and retry policy |
| CI/CD | Approved repository pipeline and deployment mechanism | Review gates, secret handling, rollback, environment promotion and auditability |


The PRD does not commit the organization to a cloud provider or cost before a technical/commercial comparison. A deployment decision should include an estimated monthly operating cost, one-time setup effort, backup/restore design and ownership of patching and incidents.


# 15. Deployment, Environments and Operations


## 15.1 Environments


| Environment | Purpose | Data / access rules |
| --- | --- | --- |
| Development | Daily engineering and integration work | Synthetic data; developer access; no production secrets |
| Test / QA | Automated and manual validation | Synthetic or anonymized data; repeatable test fixtures |
| Staging / UAT | Business acceptance and release rehearsal | Production-like configuration; controlled access; approved test data |
| Production | Live employee and business operations | Restricted access, approved release process, monitoring, backups and incident ownership |



## 15.2 CI/CD and Release Controls

- Build and test on each pull request or approved change.
- Run unit/integration tests, static analysis and dependency checks.
- Require code review and protected-branch rules for production-bound changes.
- Deploy to staging before production; obtain business acceptance where required.
- Apply database migrations in a controlled, reversible or forward-fix-aware manner.
- Use health checks and post-deployment smoke tests.
- Maintain a rollback/roll-forward plan and identify the release owner.
- Keep environment-specific configuration and secrets outside source code.

## 15.3 Backup and Recovery

The team must define backup frequency, retention, encryption, access control, cross-zone/region strategy where appropriate, and restore ownership. Backup completion alone is not sufficient: restore tests must verify data integrity and application usability.


| Item | Requirement |
| --- | --- |
| RPO | Maximum acceptable data loss window — TBD with management and business owners. |
| RTO | Maximum acceptable service recovery time — TBD with management and business owners. |
| Backup policy | Frequency, retention, encryption, storage location and deletion controls — to be approved. |
| Restore testing | Documented restore drill before go-live and at a recurring approved cadence. |
| Incident response | Named escalation contacts, severity definitions, communications and recovery runbook. |



## 15.4 Monitoring and Support

- Monitor application health, API error rates, latency, database health, disk/storage capacity and certificate/secret expiry where applicable.
- Use structured logs with correlation IDs and defined retention.
- Alert on service unavailability, repeated failures, backup failures and security-relevant events.
- Define severity levels, response ownership, escalation paths and support hours.
- Document deployment, rollback, database restore, credential rotation and incident triage runbooks.

# 16. Testing Strategy and Acceptance Criteria


## 16.1 Test Layers


| Test layer | Coverage |
| --- | --- |
| Unit tests | Business rules, validations, date calculations, leave balance logic, payroll formulas and permission utilities. |
| Integration tests | Spring services, repository/database interactions, transaction boundaries, migrations and API behaviour. |
| API contract tests | Request validation, response schemas, status codes, pagination, error formats and authorization. |
| End-to-end tests | Employee onboarding, attendance regularization, leave approval, payroll preparation and payslip access. |
| Security tests | Authentication, role/record access, direct API abuse, injection, session/token handling and sensitive export restrictions. |
| Performance tests | Representative list, search, report, dashboard and payroll workloads using agreed data volumes. |
| Recovery tests | Backup restoration, deployment rollback/roll-forward and recovery runbooks. |
| UAT | HR, Finance, managers and selected employees validate approved workflows against acceptance criteria. |



## 16.2 Release Acceptance Gates

- All P0 requirements have linked, passing test cases or an explicitly approved exception.
- No unresolved critical security defect; high-risk exceptions require formal documented acceptance.
- Payroll test scenarios reconcile exactly according to approved rounding and calculation rules.
- Role and record-level access tests demonstrate that users cannot access unauthorized records through direct API calls.
- Database migrations have been tested against a representative environment and data volume.
- Backup restore and incident/recovery procedures have been rehearsed.
- Monitoring, alerting, logs and operational ownership are in place.
- HR and Finance sign off on relevant business rules; management approves go-live readiness.
- Known issues, support ownership, rollback plan and release notes are documented.

## 16.3 Essential Test Scenarios

- Duplicate employee ID; invalid department reference; employee deactivation with historical records.
- Manager attempts to view a non-team employee; employee attempts to retrieve another employee's payslip.
- Two approvers act on the same request concurrently; only one valid terminal transition is recorded.
- Leave requests overlap; balance is insufficient; approver is inactive; request is cancelled after approval.
- Attendance crosses midnight; punch is missing; duplicate import row exists; regularization is approved.
- Salary structure changes mid-period; employee joins or leaves mid-period; a payroll input is missing.
- Payroll calculation is retried; run is finalized; post-finalization correction is attempted.
- Database connection fails during a transaction; transaction rolls back without partial balance or payroll updates.
- Backup restore is performed; restored application can authenticate and retrieve reconciled test data.
- API receives malformed input, oversized upload or repeated authentication attempts.

# 17. Delivery Roadmap and Governance


| Phase | Primary deliverables | Exit / approval gate |
| --- | --- | --- |
| 0 — Discovery and decisions | Approved goals, multi-tenant SaaS architecture decision, user roles, policy workshops, scope, business rules, deployment comparison and risk register | Management, HR and Finance scope & architecture sign-off |
| 1 — Foundation | Repository, multi-tenant architecture baseline, tenant isolation context, database migrations, API standards, authentication/authorization foundation, feature entitlement model, policy versioning core, CI/CD and environments | Security, multi-tenancy and architecture review |
| 2 — Organization and employee core | Organization setup, employee lifecycle, user mapping, onboarding and audit events | Employee workflow UAT |
| 3 — Attendance and leave | Shifts, attendance records/import foundation, regularization, leave policies/balances/approvals | HR workflow UAT and reconciliation |
| 4 — Payroll and payslips | Salary structures, approved calculations, payroll runs, exceptions, finalization and payslips | Finance rule approval and payroll reconciliation |
| 5 — Reporting and hardening | Reports, notifications, performance/security testing, monitoring, backup and recovery drills | QA and production readiness review |
| 6 — Pilot and go-live | Controlled pilot, issue resolution, training, migration, support handover and launch | Formal go-live approval |
| Later releases | Approved integrations, advanced reporting and additional HR capabilities | Separate prioritization and change approval |



## 17.1 Governance and Ownership


| Role | Accountability |
| --- | --- |
| Management Sponsor | Business priority, budget, scope trade-offs and final go-live approval. |
| Product Owner / IT Lead | Requirements baseline, backlog prioritization, cross-team coordination, risks and delivery reporting. |
| HR Process Owner | Employee lifecycle, attendance, leave and HR policy decisions. |
| Finance / Payroll Owner | Salary components, payroll formulas, reconciliation and statutory rule validation. |
| Technical Lead / Backend | Architecture, APIs, database integrity, security implementation and technical documentation. |
| Frontend Engineering | Accessible UI, API integration, validation, responsive behaviour and frontend tests. |
| QA / Test Owner | Test strategy, traceability, regression coverage, defect triage and release evidence. |
| Cloud / Operations Owner | Hosting, deployments, monitoring, backups, access and incident response. |



## 17.2 Change Control

Every material change to approved scope should include the business rationale, affected requirement IDs, priority, effort and schedule impact, dependencies, risks, acceptance criteria and approving owner. Unapproved additions should not be treated as committed delivery.


# 18. Risks, Dependencies and Mitigations


| Risk / dependency | Impact | Mitigation / owner |
| --- | --- | --- |
| HR policies not finalized | Rework or incorrect leave/attendance behaviour | HR process owner approves rules before implementation. |
| Payroll requirements incomplete | Incorrect pay outcomes and compliance exposure | Finance validates rules, examples, effective dates and test cases. |
| Cloud provider undecided | Cost or architecture rework | Complete a weighted comparison before production environment commitment. |
| Scope creep | Delivery delay and reduced quality | P0/P1/P2 backlog and formal change control. |
| Sensitive data mishandled | Privacy/security incident | Data classification, least privilege, encryption, audit and restricted exports. |
| Insufficient test coverage | Production defects or inconsistent balances | Requirement traceability, automated tests, UAT and release gates. |
| Data migration quality | Duplicate or incomplete employee records | Data profiling, mapping, validation, dry runs and reconciliation if migration is in scope. |
| Integration provider instability | Workflow delays or duplicate events | Timeouts, retries, idempotency, monitoring and manual reconciliation path. |
| No recovery rehearsal | Long outage or data loss | Approved RPO/RTO, scheduled restore drills and runbooks. |
| Unclear operational ownership | Unresolved incidents and maintenance gaps | Assign service owner, support hours, escalation and release responsibility before go-live. |



# 19. Assumptions, Open Decisions and Change Control


## 19.1 Current Planning Assumptions

- The product is intended for production use and must use a backend and persistent relational database.
- React/Vite, Spring Boot/Java 21 and PostgreSQL are the proposed baseline, subject to technical approval.
- The first release focuses on core HRMS workflows rather than every possible HR software category.
- Payroll functionality must be driven by approved company policy and validated applicable requirements.
- Cloud hosting will be selected after security, regional availability, operations and cost review.
- The application must support role-based and record-level access from the first production release.
- Development and test environments should use synthetic or anonymized data.

## 19.2 Open Decisions Register


| ID | Decision required | Owner | Status |
| --- | --- | --- | --- |
| OD-01 | Single-company deployment or multi-tenant product architecture — Resolved: Multi-tenant SaaS with shared database, row-level tenant isolation, and tenant-scoped configuration. | Management / IT | Approved / Closed |
| OD-02 | AWS vs Airtel Cloud vs other provider; region and hosting model | IT / Management | Open |
| OD-03 | Authentication approach, SSO/MFA requirements and account recovery | IT / Security | Open |
| OD-04 | Approved employee fields, mandatory documents and onboarding workflow | HR | Open |
| OD-05 | Attendance rules, shift patterns, grace periods and import format | HR | Open |
| OD-06 | Leave categories, accrual, carry-forward, expiry and approval matrix | HR | Open |
| OD-07 | Salary components, payroll periods, proration and rounding | Finance | Open |
| OD-08 | Applicable statutory rules and external expert validation | Finance / HR | Open |
| OD-09 | Required reports and sensitive-field visibility | Management / HR / Finance | Open |
| OD-10 | Data retention, backups, RPO/RTO and incident response | IT / Management | Open |
| OD-11 | Initial data migration scope and source systems | HR / IT | Open |
| OD-12 | Go-live date, pilot group, support hours and service targets | Management / Product Owner | Open |



## 19.3 Decision Management

Open decisions must be assigned an owner and target date during planning. Decisions affecting payroll, privacy, security, data architecture or cloud cost must be resolved before the related implementation is considered production-ready.


# 20. Acceptance, Handover and Sign-Off


## 20.1 Product Acceptance

Product acceptance requires agreement that the approved P0 scope is implemented, tested, documented and demonstrated; role-based access and data integrity are verified; HR and Finance approve their relevant business rules; and operational readiness gates are met.


## 20.2 Handover Package

- Approved PRD and change log.
- Architecture diagram and key architecture decisions.
- Database ERD and migration history.
- API specification and integration notes.
- Role-permission matrix and security configuration guidance.
- Test plan, test results, defects and UAT sign-off.
- Deployment and rollback runbooks.
- Backup/restore and incident-response runbooks.
- Administrator and user guides.
- Known limitations, support contacts and future backlog.

## 20.3 Sign-Off


| Stakeholder | Approval responsibility | Name / signature / date |
| --- | --- | --- |
| Management Sponsor | Business goals, scope, priorities and go-live readiness | Pending |
| HR Process Owner | Employee, attendance and leave requirements | Pending |
| Finance / Payroll Owner | Payroll rules, calculations and reconciliation | Pending |
| IT Lead / Product Owner | Requirements baseline, delivery plan and coordination | Pending |
| Technical / Operations Owner | Architecture, security, deployment and recovery readiness | Pending |


Approval statement: Sign-off confirms the agreed scope and requirements baseline. It does not waive applicable security, privacy, financial-control or statutory obligations.


# Appendix A — Requirement ID Catalogue

Requirement IDs are stable references for backlog items, implementation tickets, test cases, defect reports and change requests. Do not renumber IDs after they are referenced by downstream artifacts.


| Prefix | Domain |
| --- | --- |
| FR-IAM | Identity, authentication and authorization |
| FR-ORG / FR-EMP | Organization and employee lifecycle |
| FR-ATT | Attendance and shifts |
| FR-LEV | Leave management |
| FR-PAY | Payroll and payslips |
| FR-ESS | Employee self-service |
| FR-REP | Reports and dashboards |
| FR-NOT | Notifications |
| FR-AUD / FR-ADM | Audit, administration and support |
| BR-* | Business rules |
| OD-* | Open decisions |
| NFR-* | Non-functional requirements; detailed numeric targets to be approved |



# Appendix B — Suggested API Resource Catalogue

This is a resource-level planning aid, not a final API contract. Detailed schemas, pagination conventions, error codes, authorization requirements and examples should be defined in OpenAPI before implementation.

- Authentication: /api/v1/auth
- Users and roles: /api/v1/users, /api/v1/roles, /api/v1/permissions
- Organization: /api/v1/organizations, /api/v1/branches, /api/v1/departments, /api/v1/designations
- Employees: /api/v1/employees, /api/v1/employees/{id}/documents
- Attendance: /api/v1/attendance, /api/v1/attendance/imports, /api/v1/attendance-requests
- Shifts and calendars: /api/v1/shifts, /api/v1/shift-assignments, /api/v1/holidays
- Leave: /api/v1/leave-types, /api/v1/leave-policies, /api/v1/leave-balances, /api/v1/leave-requests
- Payroll: /api/v1/salary-components, /api/v1/salary-structures, /api/v1/payroll-periods, /api/v1/payroll-runs
- Payslips: /api/v1/payslips
- Reports: /api/v1/reports/employees, /attendance, /leave, /payroll
- Notifications: /api/v1/notifications
- Audit: /api/v1/audit-events
- Operational health: /actuator/health with production-safe exposure configuration

# Appendix C — Production Readiness Checklist

- [ ] Management, HR and Finance have approved the release scope and applicable rules.
- [ ] All P0 requirements are traceable to implementation and passing tests.
- [ ] Role and record-level authorization tests pass for all sensitive modules.
- [ ] Payroll scenarios reconcile with Finance-approved examples.
- [ ] Database migrations, indexes and constraints are reviewed and tested.
- [ ] Secrets are externalized; no production credentials are present in source control.
- [ ] TLS, encryption, access controls and log redaction are verified.
- [ ] Dependency/security scans are reviewed and release-blocking findings resolved.
- [ ] Backups are configured and a restore test is evidenced.
- [ ] Monitoring, alerting, health checks and incident ownership are active.
- [ ] Deployment and rollback/roll-forward procedures are tested.
- [ ] UAT, user training, support documentation and known-issue register are complete.
- [ ] Data migration is reconciled if migration is in scope.
- [ ] Final go-live approval is recorded.

# Appendix D — Vendor Feature Scope & Release Classification Matrix

Purpose: compare the supplied HRMS vendor feature catalogue with the current production PRD baseline. This matrix prevents vendor capability from being mistaken for an approved delivery commitment.

Classification rules

- Included in Phase 1 — explicitly part of the proposed core production baseline. Detailed business rules and acceptance criteria still require the relevant owner’s approval.
- Planned for Phase 2/3 — not committed to the initial release; subject to prioritization, dependencies, estimates and formal approval.
- Out of Scope / Not Committed — not part of the current roadmap unless a change request is approved.
Important boundary: a module name appearing in the PRD does not mean every advanced feature in that module is included. In particular, basic payroll is not equivalent to validated statutory tax computation or automated government filing.


## ESS Web Portal & Mobile App


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| ESS web portal for employee self-service | Included in Phase 1 | Core employee self-service is in the baseline; exact screens and permissions follow approved ESS requirements. |
| Dedicated native mobile app / mobile-specific workflows | Planned for Phase 2/3 | A dedicated app is not a Phase 1 deliverable; mobile-responsive web is the initial approach unless approved otherwise. |



## Core HR


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Multiple entities / SBUs with separate configuration | Included in Phase 1 | Tenant context, multi-entity / SBU hierarchy, and tenant-scoped configuration baselined in Phase 1 foundation per Architecture Review. |
| Multiple branches | Included in Phase 1 | Multi-branch management with location-specific calendars and policy assignments baselined in Phase 1 foundation. |
| Department management | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Role and access management | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Custom field management | Planned for Phase 2/3 | Advanced configurability is not explicitly committed in the current Phase 1 requirements. |
| HCM setup / organization configuration | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Footer and signature setup | Planned for Phase 2/3 | Advanced configurability is not explicitly committed in the current Phase 1 requirements. |



## Employee Management


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Employee onboarding workflow | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Bulk import employee details | Planned for Phase 2/3 | Not fully specified in the Phase 1 acceptance criteria; schedule as a later enhancement unless HR approves it for the initial release. |
| Organization chart visualization | Planned for Phase 2/3 | Not fully specified in the Phase 1 acceptance criteria; schedule as a later enhancement unless HR approves it for the initial release. |
| Employee hierarchy chart | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Employee profile management | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Employee self-onboarding | Planned for Phase 2/3 | Not fully specified in the Phase 1 acceptance criteria; schedule as a later enhancement unless HR approves it for the initial release. |
| Employee profile update approval workflow | Planned for Phase 2/3 | Not fully specified in the Phase 1 acceptance criteria; schedule as a later enhancement unless HR approves it for the initial release. |
| Employee job description management | Planned for Phase 2/3 | Not fully specified in the Phase 1 acceptance criteria; schedule as a later enhancement unless HR approves it for the initial release. |
| Employee documentation management | Planned for Phase 2/3 | Not fully specified in the Phase 1 acceptance criteria; schedule as a later enhancement unless HR approves it for the initial release. |



## Attendance Management


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Attendance policy setup | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Overtime policy setup | Planned for Phase 2/3 | Advanced attendance rule or device/mobile capability not fully baselined; confirm policy, device/provider and acceptance tests before scheduling. |
| Attendance email notifications | Planned for Phase 2/3 | Advanced attendance rule or device/mobile capability not fully baselined; confirm policy, device/provider and acceptance tests before scheduling. |
| Attendance import / bulk upload | Planned for Phase 2/3 | Advanced attendance rule or device/mobile capability not fully baselined; confirm policy, device/provider and acceptance tests before scheduling. |
| Rotational shift management | Planned for Phase 2/3 | Advanced attendance rule or device/mobile capability not fully baselined; confirm policy, device/provider and acceptance tests before scheduling. |
| Flexi shifts | Planned for Phase 2/3 | Advanced attendance rule or device/mobile capability not fully baselined; confirm policy, device/provider and acceptance tests before scheduling. |
| Night shift management | Planned for Phase 2/3 | Advanced attendance rule or device/mobile capability not fully baselined; confirm policy, device/provider and acceptance tests before scheduling. |
| Roster / work schedule management | Planned for Phase 2/3 | Advanced attendance rule or device/mobile capability not fully baselined; confirm policy, device/provider and acceptance tests before scheduling. |
| Geo-fencing | Planned for Phase 2/3 | Advanced attendance rule or device/mobile capability not fully baselined; confirm policy, device/provider and acceptance tests before scheduling. |
| Geo-location validation | Planned for Phase 2/3 | Advanced attendance rule or device/mobile capability not fully baselined; confirm policy, device/provider and acceptance tests before scheduling. |
| Attendance correction and regularization | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Employee movement register | Planned for Phase 2/3 | Advanced attendance rule or device/mobile capability not fully baselined; confirm policy, device/provider and acceptance tests before scheduling. |
| Biometric integration | Planned for Phase 2/3 | Advanced attendance rule or device/mobile capability not fully baselined; confirm policy, device/provider and acceptance tests before scheduling. |
| Automatic present marking | Planned for Phase 2/3 | Advanced attendance rule or device/mobile capability not fully baselined; confirm policy, device/provider and acceptance tests before scheduling. |
| Multiple punch-in handling | Planned for Phase 2/3 | Advanced attendance rule or device/mobile capability not fully baselined; confirm policy, device/provider and acceptance tests before scheduling. |
| Web app punch-in | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Mobile app punch-in | Planned for Phase 2/3 | Advanced attendance rule or device/mobile capability not fully baselined; confirm policy, device/provider and acceptance tests before scheduling. |
| Selfie-based punch-in | Planned for Phase 2/3 | Advanced attendance rule or device/mobile capability not fully baselined; confirm policy, device/provider and acceptance tests before scheduling. |



## Leave Management


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Leave policy setup | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Leave clubbing rules | Planned for Phase 2/3 | Specific rule configuration is not fully specified in the Phase 1 baseline; requires HR policy confirmation and test cases. |
| Leave entitlement configuration | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Holiday master | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Holiday allocation list | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Leave application import | Planned for Phase 2/3 | Specific rule configuration is not fully specified in the Phase 1 baseline; requires HR policy confirmation and test cases. |
| Leave carry-forward rules | Planned for Phase 2/3 | Specific rule configuration is not fully specified in the Phase 1 baseline; requires HR policy confirmation and test cases. |
| Leave encashment rules | Planned for Phase 2/3 | Specific rule configuration is not fully specified in the Phase 1 baseline; requires HR policy confirmation and test cases. |
| Leave sandwich rules | Planned for Phase 2/3 | Specific rule configuration is not fully specified in the Phase 1 baseline; requires HR policy confirmation and test cases. |
| Notice-period leave rules | Planned for Phase 2/3 | Specific rule configuration is not fully specified in the Phase 1 baseline; requires HR policy confirmation and test cases. |
| Compensatory-off request | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Compensatory-off payment | Planned for Phase 2/3 | Specific rule configuration is not fully specified in the Phase 1 baseline; requires HR policy confirmation and test cases. |
| Leave approval workflows | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Leave application restrictions | Planned for Phase 2/3 | Specific rule configuration is not fully specified in the Phase 1 baseline; requires HR policy confirmation and test cases. |



## Payroll Management


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Company / SBU-wise payroll setup | Planned for Phase 2/3 | Advanced payroll or statutory functionality not fully specified in the initial baseline; Finance validation and approved calculation rules are required. |
| Payroll cycle setup | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Compliance branch setup | Planned for Phase 2/3 | Advanced payroll or statutory functionality not fully specified in the initial baseline; Finance validation and approved calculation rules are required. |
| Pay elements setup | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Automatic location-wise payroll compliance | Planned for Phase 2/3 | Advanced payroll or statutory functionality not fully specified in the initial baseline; Finance validation and approved calculation rules are required. |
| Salary structure templates | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Employee salary details | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Employee compliance setup | Planned for Phase 2/3 | Advanced payroll or statutory functionality not fully specified in the initial baseline; Finance validation and approved calculation rules are required. |
| Payroll adjustments | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Incentives / retention bonus management | Planned for Phase 2/3 | Advanced payroll or statutory functionality not fully specified in the initial baseline; Finance validation and approved calculation rules are required. |
| Salary increment arrears | Planned for Phase 2/3 | Advanced payroll or statutory functionality not fully specified in the initial baseline; Finance validation and approved calculation rules are required. |
| One-click payroll processing | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Salary hold | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Import previous-month salary data | Planned for Phase 2/3 | Advanced payroll or statutory functionality not fully specified in the initial baseline; Finance validation and approved calculation rules are required. |
| Form 24Q | Planned for Phase 2/3 | Advanced payroll or statutory functionality not fully specified in the initial baseline; Finance validation and approved calculation rules are required. |



## TDS & Income Tax


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| TDS cycle setup | Planned for Phase 2/3 | Requires a separately approved tax specification, current rules, Finance validation, effective-date handling and statutory test cases. |
| Income-tax regime selection | Planned for Phase 2/3 | Requires a separately approved tax specification, current rules, Finance validation, effective-date handling and statutory test cases. |
| Automatic TDS calculation | Planned for Phase 2/3 | Requires a separately approved tax specification, current rules, Finance validation, effective-date handling and statutory test cases. |
| Investment declaration and proof management | Planned for Phase 2/3 | Requires a separately approved tax specification, current rules, Finance validation, effective-date handling and statutory test cases. |
| Tax projection and simulation | Planned for Phase 2/3 | Requires a separately approved tax specification, current rules, Finance validation, effective-date handling and statutory test cases. |
| Form 16 generation and consolidated pay sheet | Planned for Phase 2/3 | Requires a separately approved tax specification, current rules, Finance validation, effective-date handling and statutory test cases. |
| F&F TDS computation | Planned for Phase 2/3 | Requires a separately approved tax specification, current rules, Finance validation, effective-date handling and statutory test cases. |
| Perquisite management | Planned for Phase 2/3 | Requires a separately approved tax specification, current rules, Finance validation, effective-date handling and statutory test cases. |



## Loans & Advances


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Salary advance / loan policy setup | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Salary advance / loan request workflow | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Interest and charge configuration | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Loan integration with payroll | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Disbursement and EMI configuration | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Loan balance and statement tracking | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |



## Expense Management


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Expense policy setup | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Expense claim submission | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Expense claim approval workflow | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Expense reports | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |



## Travel Management


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Travel policy setup | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Travel request and approval workflow | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Travel advance management | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Travel booking | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Travel expense payment submission | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |



## Confirmation & Probation


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Confirmation / probation process setup | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Automated probation tracking | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Configurable confirmation workflow | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Confirmation evaluation form | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Confirmation decision and actions | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Confirmation notifications and reminders | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Document and letter automation for confirmation | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Contractor renewal tracking | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |



## Promotion & Transfer Management


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Centralized promotion and transfer workflow | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Promotion management | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Transfer management | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Promotion / transfer communication and letters | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Promotion / transfer audit and history | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Bulk promotion / transfer option | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |



## OKR & Goals


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Define OKR cycle | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Assign OKR cycle to employees | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| OKR templates | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| OKR review frequency setup | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| OKR / goal setup and approval workflow | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| OKR / goal review and approval workflow | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| OKR notifications and reminders | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Track OKR progress | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| OKR dashboards | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| OKR reports | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |



## Separation Management


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Separation process setup | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Separation workflow setup | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Exit interview and feedback | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Exit checklist automation | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Full and final settlement workflow | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Separation analytics and reporting | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |



## Document Management


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Payslip generation | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| HR letter template setup | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| HR policy document upload | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Digital employee letter repository | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Employee documentation audit | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |



## Employee Helpdesk


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Employee helpdesk setup | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Configurable query categories and SLAs | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Employee query / ticket management | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| SLA tracking and escalation | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Helpdesk analytics and reports | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |



## Task Management


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Customized task template setup | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Centralized task assignment and tracking | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Progress tracking and status management | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Daily task management | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Task reports and analytics | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |



## Timesheet Management


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Customized timesheet template setup | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Project-wise time tracking | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Automated timesheet workflow | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Billability and utilization tracking | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Timesheet integration with payroll and attendance | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Timesheet reports and analytics | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |



## Email Notification


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| SMTP setup | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Email notification setup | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Timed reminders | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Mail event setup | Included in Phase 1 | Part of the proposed core production baseline; detailed rules and acceptance criteria still require owner sign-off. |
| Compose and send email from the application | Planned for Phase 2/3 | A configurable compose-email feature is not explicitly included in the initial baseline; core system notifications remain in scope. |



## Offer & Pre-Onboarding


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Offer letter template setup | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Digital offer and candidate management | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Pre-joining data collection | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Joining formalities automation | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Joining and compliance form automation | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |



## Asset Management


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Centralized asset repository | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Asset request and return workflow | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Employee-wise asset allocation | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Asset lifecycle tracking | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Inventory and stock management | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Asset condition and maintenance tracking | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |
| Asset renewal and insurance due dates | Planned for Phase 2/3 | Not part of the initial release baseline. Requires prioritization, detailed requirements, dependencies and approval before commitment. |



## Learning Management


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Centralized learning hub | Out of Scope / Not Committed | Learning management is outside the current core HRMS roadmap and requires a separate business case and approval. |
| Course creation and management | Out of Scope / Not Committed | Learning management is outside the current core HRMS roadmap and requires a separate business case and approval. |
| Learning calendar and notifications | Out of Scope / Not Committed | Learning management is outside the current core HRMS roadmap and requires a separate business case and approval. |
| Learning paths and skill development | Out of Scope / Not Committed | Learning management is outside the current core HRMS roadmap and requires a separate business case and approval. |
| Assessments and certifications | Out of Scope / Not Committed | Learning management is outside the current core HRMS roadmap and requires a separate business case and approval. |



## Master Reports — Attendance & Leave Reports


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Monthly attendance report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Daily present / absent report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Attendance muster roll report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Attendance muster roll with leave-code details | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Attendance penalty report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Attendance summary report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Employee attendance report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Missing punch report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Geo-fencing reports | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Leave report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Compensatory-off report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Applied leave report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Leave balance report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Monthly leave report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Employee late-coming / early-going report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Work-schedule assignment details | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |



## Master Reports — Statutory & Enrollment Reports


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| LWF statement | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| LWF Form A-1 | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| PF statement | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| PF register | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| PF return | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| ESIC statement | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| ESIC return | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| ESIC register | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| PT statement | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| PT Form 5 | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| New-joiner EPF enrollment report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| New-joiner ESIC enrollment report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| NPS statement | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Gratuity projection | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Form D — Equal Remuneration Act | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Form D — Equal Remuneration Act gender-wise | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |



## Master Reports — Salary, Payroll & Arrears Reports


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Employee-type-wise salary sheet | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Employee-type-wise salary register with overtime and penalty | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Employee-type-wise salary register | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Employee-type-wise CTC register | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Employee-type-wise salary comparison sheet | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Department-wise salary summary report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| JV salary report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| JV reimbursement report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| SBU and branch-wise summary report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| SBU-wise salary register report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Branch-wise salary register report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Branch-wise salary summary report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| SBU-wise salary summary report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Payslips | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Employee salary structure | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Monthly salary summary report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| New-joiner monthly salary report, SBU-wise | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Left-employee monthly salary report, SBU-wise | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Employee-type-wise CTC salary register | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Employee-type-wise salary register with arrears | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Arrears summary report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Monthly arrears disbursement report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Net payable summary report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |



## Master Reports — Banking & Remittance Reports


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| HDFC bank report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Pay-type-wise HDFC bank report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Bank-wise bank report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Bank report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Bank balance credit report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Element-wise bank report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Bank remittance report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Bank statement report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Department and sub-department-wise bank report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |



## Master Reports — Tax Reports


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Projected TDS report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Month-wise TDS liability report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Monthly TDS statement | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| TDS declaration report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Monthly TDS report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| HRA declaration report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Housing-loan declaration report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |



## Master Reports — Employee, Organization & Administration Reports


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Role assignment list | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Organization-wise headcount details | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Employee probation details | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Employee special-moment report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Pending reporting-person assignment | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Manpower detail report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Organization education detail | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Organization gender detail | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Employee joining and left-service report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Employee growth history | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Leave encashment report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Organization age-wise detail | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Organization experience-wise detail | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Employee history | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Employee login report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Credential and login report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Ticket report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| PMS status report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Employee defaulter report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Employee-wise weekly timesheet report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |
| Monthly timesheet report | Planned for Phase 2/3 | The PRD currently commits to basic operational reporting, not this full named report catalogue. Each report needs fields, filters, permissions, export format and reconciliation tests approved. |



## Reporting Baseline


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Basic employee / attendance / leave / payroll operational reports required for Phase 1 | Included in Phase 1 | Baseline reports are limited to the approved core workflows; do not assume every vendor-listed report is included. |



## Cross-Module Capabilities


| Feature / Capability | Scope Status | Scope Note |
| --- | --- | --- |
| Configurable approval workflows across all modules | Included in Phase 1 (Core Engine) / Phase 2 (Visual Designer) | Configurable multi-step approval workflows (manager/HR chains, delegation, escalation) included in Phase 1 foundation; visual drag-and-drop workflow designer planned for Phase 2. |
| Complete business-process history for every module | Planned for Phase 2/3 | General audit logs are in baseline; complete module-specific business history is a later enhancement. |
| Full audit history for document and promotion / transfer events | Planned for Phase 2/3 | Specialized audit views are not fully specified in the initial baseline. |
| Advanced dashboards and workforce analytics | Planned for Phase 2/3 | Basic operational visibility is in scope; advanced analytics and configurable dashboards are later-release candidates. |
| Direct statutory filing / submission to government portals | Out of Scope / Not Committed | Automatic legal/tax filing is explicitly excluded unless validated rules, authorized integrations and formal approval are provided. |
| Bank payment execution / direct salary disbursement | Out of Scope / Not Committed | Direct bank payment execution is not part of the current initial-release commitment. |



## Management decision required

Approve the Phase 1 baseline, confirm whether multi-entity/branch support and advanced employee workflows are required at launch, and assign owners for Phase 2/3 modules. TDS/statutory calculations must not be declared production-ready until Finance validates the applicable rules and test cases. Any change to these classifications must follow the PRD change-control process.
