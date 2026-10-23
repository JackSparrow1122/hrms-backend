package com.gahrms.backend.modules.organization.service;

import com.gahrms.backend.common.dto.PageResponse;
import com.gahrms.backend.common.exception.ConflictException;
import com.gahrms.backend.common.exception.ResourceNotFoundException;
import com.gahrms.backend.modules.employee.repository.EmployeeRepository;
import com.gahrms.backend.modules.identity.entity.Role;
import com.gahrms.backend.modules.identity.entity.ScopeType;
import com.gahrms.backend.modules.identity.entity.User;
import com.gahrms.backend.modules.identity.repository.RoleRepository;
import com.gahrms.backend.modules.identity.repository.UserRepository;
import com.gahrms.backend.modules.organization.dto.CreateTenantRequest;
import com.gahrms.backend.modules.organization.dto.TenantDto;
import com.gahrms.backend.modules.organization.dto.UpdateTenantRequest;
import com.gahrms.backend.modules.employee.entity.Employee;
import com.gahrms.backend.modules.employee.entity.EmploymentStatus;
import com.gahrms.backend.modules.organization.entity.Branch;
import com.gahrms.backend.modules.organization.entity.Company;
import com.gahrms.backend.modules.organization.entity.Department;
import com.gahrms.backend.modules.organization.entity.Designation;
import com.gahrms.backend.modules.organization.entity.TenantStatus;
import com.gahrms.backend.modules.organization.repository.BranchRepository;
import com.gahrms.backend.modules.organization.repository.CompanyRepository;
import com.gahrms.backend.modules.organization.repository.DepartmentRepository;
import com.gahrms.backend.modules.organization.repository.DesignationRepository;
import com.gahrms.backend.modules.organization.repository.TenantSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final jakarta.persistence.EntityManager entityManager;

    @Transactional
    public TenantDto createTenant(CreateTenantRequest request) {
        log.info("Creating customer tenant organization: {} ({})", request.getName(), request.getCode());

        String codeUpper = request.getCode().trim().toUpperCase();
        if (companyRepository.existsByCode(codeUpper)) {
            throw new ConflictException("A company already exists with code: " + codeUpper);
        }
        if (companyRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new ConflictException("A company already exists with name: " + request.getName());
        }
        if (userRepository.existsByEmailIgnoreCase(request.getAdminEmail().trim())) {
            throw new ConflictException("A user account already exists with email: " + request.getAdminEmail());
        }

        // 1. Create Company / Tenant Record
        Company company = Company.builder()
                .name(request.getName().trim())
                .code(codeUpper)
                .registrationNumber(request.getRegistrationNumber())
                .taxId(request.getTaxId())
                .timezone(StringUtils.hasText(request.getTimezone()) ? request.getTimezone() : "Asia/Kolkata")
                .currency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency() : "INR")
                .status(TenantStatus.ACTIVE)
                .subscriptionPlan(StringUtils.hasText(request.getSubscriptionPlan()) ? request.getSubscriptionPlan() : "ENTERPRISE")
                .maxEmployees(request.getMaxEmployees() != null ? request.getMaxEmployees() : 500)
                .maxBranches(request.getMaxBranches() != null ? request.getMaxBranches() : 5)
                .contactEmail(request.getContactEmail() != null ? request.getContactEmail().trim().toLowerCase() : request.getAdminEmail().trim().toLowerCase())
                .contactPhone(request.getContactPhone())
                .domain(request.getDomain())
                .build();

        Company savedCompany = companyRepository.save(company);
        log.info("Created tenant company id: {}", savedCompany.getId());

        // 2. Provision Default Headquarters Branch
        Branch hqBranch = Branch.builder()
                .company(savedCompany)
                .name(savedCompany.getName() + " Headquarters")
                .code("HQ-01")
                .city("Corporate")
                .state("Corporate")
                .country("India")
                .headquarters(true)
                .build();
        branchRepository.save(hqBranch);

        // 3. Provision Core Departments
        Department execDept = Department.builder()
                .company(savedCompany)
                .name("Executive Leadership")
                .code("DEP-EXEC")
                .active(true)
                .build();
        Department hrDept = Department.builder()
                .company(savedCompany)
                .name("Human Resources")
                .code("DEP-HR")
                .active(true)
                .build();
        Department itDept = Department.builder()
                .company(savedCompany)
                .name("Information Technology")
                .code("DEP-IT")
                .active(true)
                .build();
        departmentRepository.saveAll(List.of(execDept, hrDept, itDept));

        // 4. Provision Initial Organization-wide Designations
        Designation hrDirDesig = Designation.builder()
                .company(savedCompany)
                .name("Director of Human Resources")
                .code("DES-HRD")
                .level(8)
                .active(true)
                .build();
        Designation mgrDesig = Designation.builder()
                .company(savedCompany)
                .name("Department Manager")
                .code("DES-MGR")
                .level(6)
                .active(true)
                .build();
        Designation tlDesig = Designation.builder()
                .company(savedCompany)
                .name("Team Lead")
                .code("DES-TL")
                .level(4)
                .active(true)
                .build();
        Designation sseDesig = Designation.builder()
                .company(savedCompany)
                .name("Senior Software Engineer")
                .code("DES-SSE")
                .level(3)
                .active(true)
                .build();
        Designation seDesig = Designation.builder()
                .company(savedCompany)
                .name("Software Engineer")
                .code("DES-SE")
                .level(2)
                .active(true)
                .build();
        Designation hrExecDesig = Designation.builder()
                .company(savedCompany)
                .name("HR Executive")
                .code("DES-HRE")
                .level(2)
                .active(true)
                .build();
        designationRepository.saveAll(List.of(hrDirDesig, mgrDesig, tlDesig, sseDesig, seDesig, hrExecDesig));

        // 5. Provision Employee record for Head HR (Corporate Organization Level, no single branch restriction)
        Employee adminEmployee = Employee.builder()
                .company(savedCompany)
                .branch(null)
                .department(hrDept)
                .designation(hrDirDesig)
                .employeeCode(savedCompany.getCode() + "-0001")
                .firstName(request.getAdminFirstName().trim())
                .lastName(request.getAdminLastName().trim())
                .email(request.getAdminEmail().trim().toLowerCase())
                .phoneNumber(request.getAdminPhoneNumber())
                .status(EmploymentStatus.ACTIVE)
                .joiningDate(LocalDate.now())
                .build();
        Employee savedAdminEmployee = employeeRepository.save(adminEmployee);

        // 6. Provision Organization Head HR (ROLE_TENANT_ADMIN, Scope: ORGANIZATION)
        Role tenantAdminRole = roleRepository.findByName("ROLE_TENANT_ADMIN")
                .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_TENANT_ADMIN not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(tenantAdminRole);

        String rawPassword = StringUtils.hasText(request.getAdminPassword()) ? request.getAdminPassword().trim() : "Welcome@123";

        User adminUser = new User();
        adminUser.setEmail(request.getAdminEmail().trim().toLowerCase());
        adminUser.setPassword(passwordEncoder.encode(rawPassword));
        adminUser.setFirstName(request.getAdminFirstName().trim());
        adminUser.setLastName(request.getAdminLastName().trim());
        adminUser.setPhoneNumber(request.getAdminPhoneNumber());
        adminUser.setCompanyId(savedCompany.getId());
        adminUser.setEmployeeId(savedAdminEmployee.getId());
        adminUser.setScopeType(ScopeType.ORGANIZATION);
        adminUser.setActive(true);
        adminUser.setEmailVerified(true);
        adminUser.setRoles(roles);
        userRepository.save(adminUser);

        log.info("Provisioned HR Admin user id: {} with employee id: {} for company id: {}", adminUser.getId(), savedAdminEmployee.getId(), savedCompany.getId());

        return mapToDto(savedCompany);
    }

    @Transactional(readOnly = true)
    public PageResponse<TenantDto> getTenants(
            String search,
            TenantStatus status,
            String subscriptionPlan,
            Pageable pageable
    ) {
        Specification<Company> spec = TenantSpecification.filter(search, status, subscriptionPlan);
        Page<Company> page = companyRepository.findAll(spec, pageable);

        List<TenantDto> dtos = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return new PageResponse<>(
                dtos,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public TenantDto getTenantById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant company not found with id: " + id));
        return mapToDto(company);
    }

    @Transactional
    public TenantDto updateTenant(Long id, UpdateTenantRequest request) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant company not found with id: " + id));

        company.setName(request.getName().trim());
        if (request.getRegistrationNumber() != null) company.setRegistrationNumber(request.getRegistrationNumber());
        if (request.getTaxId() != null) company.setTaxId(request.getTaxId());
        if (request.getTimezone() != null) company.setTimezone(request.getTimezone());
        if (request.getCurrency() != null) company.setCurrency(request.getCurrency());
        if (request.getSubscriptionPlan() != null) company.setSubscriptionPlan(request.getSubscriptionPlan());
        if (request.getMaxEmployees() != null) company.setMaxEmployees(request.getMaxEmployees());
        if (request.getMaxBranches() != null) company.setMaxBranches(request.getMaxBranches());
        if (request.getContactEmail() != null) company.setContactEmail(request.getContactEmail().trim().toLowerCase());
        if (request.getContactPhone() != null) company.setContactPhone(request.getContactPhone());
        if (request.getDomain() != null) company.setDomain(request.getDomain());
        if (request.getStatus() != null) company.setStatus(request.getStatus());

        Company updated = companyRepository.save(company);
        return mapToDto(updated);
    }

    @Transactional
    public TenantDto updateTenantStatus(Long id, TenantStatus status) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant company not found with id: " + id));

        company.setStatus(status);
        Company updated = companyRepository.save(company);

        // If suspended or terminated, deactivate all tenant users
        if (status == TenantStatus.SUSPENDED || status == TenantStatus.TERMINATED) {
            List<User> users = userRepository.findByCompanyId(id);
            users.forEach(u -> u.setActive(false));
            userRepository.saveAll(users);
            log.warn("Deactivated {} users due to tenant suspension: {}", users.size(), id);
        } else if (status == TenantStatus.ACTIVE) {
            List<User> users = userRepository.findByCompanyId(id);
            users.forEach(u -> u.setActive(true));
            userRepository.saveAll(users);
            log.info("Reactivated {} users for tenant: {}", users.size(), id);
        }

        return mapToDto(updated);
    }

    @Transactional
    public void deleteTenant(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant company not found with id: " + id));

        log.info("Permanently purging tenant company ID: {} ({}) and all associated records", id, company.getName());

        // 1. Break self-referential / circular FKs
        entityManager.createNativeQuery(
                "UPDATE regularization_requests SET approver_id = NULL WHERE approver_id IN (SELECT id FROM employees WHERE company_id = :cid)"
        ).setParameter("cid", id).executeUpdate();

        entityManager.createNativeQuery(
                "UPDATE leave_requests SET approver_id = NULL WHERE approver_id IN (SELECT id FROM employees WHERE company_id = :cid)"
        ).setParameter("cid", id).executeUpdate();

        entityManager.createNativeQuery(
                "UPDATE employees SET manager_id = NULL WHERE company_id = :cid"
        ).setParameter("cid", id).executeUpdate();

        entityManager.createNativeQuery(
                "UPDATE users SET employee_id = NULL WHERE company_id = :cid"
        ).setParameter("cid", id).executeUpdate();

        // 2. Delete employee transactional records
        entityManager.createNativeQuery(
                "DELETE FROM payslips WHERE employee_id IN (SELECT id FROM employees WHERE company_id = :cid)"
        ).setParameter("cid", id).executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM salary_structures WHERE employee_id IN (SELECT id FROM employees WHERE company_id = :cid)"
        ).setParameter("cid", id).executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM employee_documents WHERE employee_id IN (SELECT id FROM employees WHERE company_id = :cid)"
        ).setParameter("cid", id).executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM regularization_requests WHERE employee_id IN (SELECT id FROM employees WHERE company_id = :cid)"
        ).setParameter("cid", id).executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM leave_requests WHERE employee_id IN (SELECT id FROM employees WHERE company_id = :cid)"
        ).setParameter("cid", id).executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM leave_balances WHERE employee_id IN (SELECT id FROM employees WHERE company_id = :cid)"
        ).setParameter("cid", id).executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM attendance_records WHERE employee_id IN (SELECT id FROM employees WHERE company_id = :cid)"
        ).setParameter("cid", id).executeUpdate();

        // 3. Delete user tokens, user roles, and users for this company
        entityManager.createNativeQuery(
                "DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE company_id = :cid)"
        ).setParameter("cid", id).executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE company_id = :cid)"
        ).setParameter("cid", id).executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM users WHERE company_id = :cid"
        ).setParameter("cid", id).executeUpdate();

        // 4. Delete employees
        entityManager.createNativeQuery(
                "DELETE FROM employees WHERE company_id = :cid"
        ).setParameter("cid", id).executeUpdate();

        // 5. Delete payroll runs, payroll periods, salary components, leave types, shifts
        entityManager.createNativeQuery(
                "DELETE FROM payroll_runs WHERE payroll_period_id IN (SELECT id FROM payroll_periods WHERE company_id = :cid)"
        ).setParameter("cid", id).executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM payroll_periods WHERE company_id = :cid"
        ).setParameter("cid", id).executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM salary_components WHERE company_id = :cid"
        ).setParameter("cid", id).executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM leave_types WHERE company_id = :cid"
        ).setParameter("cid", id).executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM shifts WHERE company_id = :cid"
        ).setParameter("cid", id).executeUpdate();

        // 6. Delete designations, departments, branches
        entityManager.createNativeQuery(
                "DELETE FROM designations WHERE company_id = :cid"
        ).setParameter("cid", id).executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM departments WHERE company_id = :cid"
        ).setParameter("cid", id).executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM branches WHERE company_id = :cid"
        ).setParameter("cid", id).executeUpdate();

        // 7. Delete company permanently
        companyRepository.delete(company);
        log.info("Tenant company ID: {} permanently purged from platform database", id);
    }

    private TenantDto mapToDto(Company company) {
        long branchCount = branchRepository.findByCompanyId(company.getId()).size();
        long deptCount = departmentRepository.findByCompanyId(company.getId()).size();
        long empCount = employeeRepository.countByCompanyId(company.getId());

        User adminUser = userRepository.findByCompanyId(company.getId()).stream()
                .filter(u -> u.isHrAdmin() || u.isTenantAdmin())
                .findFirst()
                .orElse(null);
        String adminEmail = adminUser != null ? adminUser.getEmail() : null;
        String adminName = adminUser != null ? (adminUser.getFirstName() + " " + adminUser.getLastName()).trim() : null;

        return TenantDto.builder()
                .id(company.getId())
                .name(company.getName())
                .code(company.getCode())
                .registrationNumber(company.getRegistrationNumber())
                .taxId(company.getTaxId())
                .timezone(company.getTimezone())
                .currency(company.getCurrency())
                .status(company.getStatus())
                .subscriptionPlan(company.getSubscriptionPlan())
                .maxEmployees(company.getMaxEmployees())
                .maxBranches(company.getMaxBranches())
                .contactEmail(company.getContactEmail())
                .contactPhone(company.getContactPhone())
                .domain(company.getDomain())
                .totalBranches(branchCount)
                .totalDepartments(deptCount)
                .totalEmployees(empCount)
                .adminEmail(adminEmail)
                .adminName(adminName)
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }
}
