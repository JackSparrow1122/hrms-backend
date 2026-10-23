package com.gahrms.backend.modules.employee.service;

import com.gahrms.backend.common.dto.PageResponse;
import com.gahrms.backend.common.exception.BadRequestException;
import com.gahrms.backend.common.exception.ConflictException;
import com.gahrms.backend.common.exception.ResourceNotFoundException;
import com.gahrms.backend.common.exception.UnauthorizedException;
import com.gahrms.backend.common.util.SecurityUtils;
import com.gahrms.backend.modules.employee.dto.*;
import com.gahrms.backend.modules.employee.entity.Employee;
import com.gahrms.backend.modules.employee.entity.EmploymentStatus;
import com.gahrms.backend.modules.employee.repository.EmployeeRepository;
import com.gahrms.backend.modules.employee.repository.EmployeeSpecification;
import com.gahrms.backend.modules.identity.entity.Role;
import com.gahrms.backend.modules.identity.entity.ScopeType;
import com.gahrms.backend.modules.identity.entity.User;
import com.gahrms.backend.modules.identity.repository.RoleRepository;
import com.gahrms.backend.modules.identity.repository.UserRepository;
import com.gahrms.backend.modules.organization.entity.Branch;
import com.gahrms.backend.modules.organization.entity.Company;
import com.gahrms.backend.modules.organization.entity.Department;
import com.gahrms.backend.modules.organization.entity.Designation;
import com.gahrms.backend.modules.organization.repository.BranchRepository;
import com.gahrms.backend.modules.organization.repository.CompanyRepository;
import com.gahrms.backend.modules.organization.repository.DepartmentRepository;
import com.gahrms.backend.modules.organization.repository.DesignationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final jakarta.persistence.EntityManager entityManager;

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> getEmployees(
            Long companyId,
            String search,
            Long departmentId,
            Long branchId,
            Long designationId,
            EmploymentStatus status,
            Pageable pageable
    ) {
        Long effectiveCompanyId = companyId;
        Long effectiveBranchId = branchId;
        List<Long> allowedEmployeeIds = null;
        List<Long> excludedEmployeeIds = new ArrayList<>();

        if (!SecurityUtils.isCurrentUserSuperAdmin()) {
            User currentUser = resolveCurrentUser();
            if (currentUser != null) {
                effectiveCompanyId = currentUser.getCompanyId();
                if (currentUser.getScopeType() == ScopeType.BRANCH && currentUser.getScopeBranchId() != null) {
                    effectiveBranchId = currentUser.getScopeBranchId();
                    // Branch HR Admin must strictly NEVER see Head HR (ROLE_TENANT_ADMIN) in their employee directory
                    List<Long> tenantAdminEmpIds = userRepository.findTenantAdminEmployeeIds(effectiveCompanyId);
                    if (tenantAdminEmpIds != null && !tenantAdminEmpIds.isEmpty()) {
                        excludedEmployeeIds.addAll(tenantAdminEmpIds);
                    }
                }

                // Strict hierarchical scoping: Manager & Team Lead ONLY see subordinates reporting below them
                if (currentUser.isManager() || currentUser.isTeamLead()) {
                    Long currentEmpId = currentUser.getEmployeeId();
                    if (currentEmpId == null) {
                        Employee self = employeeRepository.findByEmailIgnoreCase(currentUser.getEmail()).orElse(null);
                        if (self != null) {
                            currentEmpId = self.getId();
                        }
                    }

                    if (currentEmpId != null) {
                        if (currentUser.isManager()) {
                            allowedEmployeeIds = getSubordinateEmployeeIds(currentEmpId);
                        } else {
                            allowedEmployeeIds = getDirectReporteeIds(currentEmpId);
                        }
                    } else {
                        allowedEmployeeIds = Collections.emptyList();
                    }
                } else if (currentUser.isEmployee() && !currentUser.isTenantAdmin() && !currentUser.isHrAdmin()) {
                    // Normal employees have zero access to employee directory
                    allowedEmployeeIds = Collections.emptyList();
                }
            }
        }

        Specification<Employee> spec = EmployeeSpecification.filter(
                effectiveCompanyId, search, departmentId, effectiveBranchId, designationId, status, allowedEmployeeIds, excludedEmployeeIds
        );

        Page<Employee> employeePage = employeeRepository.findAll(spec, pageable);
        List<EmployeeResponse> responseList = employeePage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                responseList,
                employeePage.getNumber(),
                employeePage.getSize(),
                employeePage.getTotalElements(),
                employeePage.getTotalPages(),
                employeePage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        enforceTenantAccess(employee);
        return mapToResponse(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeByCode(String code) {
        Employee employee = employeeRepository.findByEmployeeCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with code: " + code));
        enforceTenantAccess(employee);
        return mapToResponse(employee);
    }

    @Transactional
    public EmployeeResponse onboardedEmployee(CreateEmployeeRequest request) {
        log.info("Onboarding new employee with email: {}", request.getEmail());

        if (!SecurityUtils.isCurrentUserSuperAdmin()) {
            User currentUser = resolveCurrentUser();
            if (currentUser != null) {
                if (currentUser.getCompanyId() != null && !currentUser.getCompanyId().equals(request.getCompanyId())) {
                    throw new UnauthorizedException("Forbidden: Cannot onboard employee to another company");
                }
                if (currentUser.getScopeType() == ScopeType.BRANCH && currentUser.getScopeBranchId() != null) {
                    if (request.getBranchId() != null && !request.getBranchId().equals(currentUser.getScopeBranchId())) {
                        throw new UnauthorizedException("Forbidden: Branch HR Admin can only onboard employees to their assigned branch location.");
                    }
                    request.setBranchId(currentUser.getScopeBranchId());
                }
            }
        }

        // 1. Email uniqueness check in both Employee and User tables
        if (employeeRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ConflictException("An employee already exists with email: " + request.getEmail());
        }
        if (request.isCreateLoginAccount() && userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ConflictException("A user account already exists with email: " + request.getEmail());
        }

        // 2. Validate Company
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + request.getCompanyId()));

        // 3. Validate optional Branch
        Branch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.getBranchId()));
            if (!branch.getCompany().getId().equals(company.getId())) {
                throw new BadRequestException("Branch does not belong to the specified company");
            }
        }

        // 4. Validate optional Department
        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));
            if (!department.getCompany().getId().equals(company.getId())) {
                throw new BadRequestException("Department does not belong to the specified company");
            }
        }

        // 5. Validate optional Designation
        Designation designation = null;
        if (request.getDesignationId() != null) {
            designation = designationRepository.findById(request.getDesignationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Designation not found with id: " + request.getDesignationId()));
            if (!designation.getCompany().getId().equals(company.getId())) {
                throw new BadRequestException("Designation does not belong to the specified company");
            }
        }

        // 6. Validate optional Reporting Manager
        Employee manager = null;
        if (request.getManagerId() != null) {
            manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + request.getManagerId()));
            if (!manager.getCompany().getId().equals(company.getId())) {
                throw new BadRequestException("Manager does not belong to the specified company");
            }
        }

        // 7. Auto-generate unique Employee Code
        String employeeCode = generateUniqueEmployeeCode(company);

        // 8. Build and save Employee (Traditional Object Creation & Setters)
        Employee employee = new Employee();
        employee.setEmployeeCode(employeeCode);
        employee.setFirstName(request.getFirstName().trim());
        employee.setLastName(request.getLastName().trim());
        employee.setEmail(request.getEmail().trim().toLowerCase());
        employee.setPhoneNumber(request.getPhoneNumber());
        employee.setDateOfBirth(request.getDateOfBirth());
        employee.setGender(request.getGender());
        employee.setJoiningDate(request.getJoiningDate());
        employee.setConfirmationDate(request.getConfirmationDate());
        employee.setStatus(request.getStatus() != null ? request.getStatus() : EmploymentStatus.ACTIVE);
        employee.setCompany(company);
        employee.setBranch(branch);
        employee.setDepartment(department);
        employee.setDesignation(designation);
        employee.setManager(manager);

        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Employee created successfully with id: {} and code: {}", savedEmployee.getId(), savedEmployee.getEmployeeCode());

        // 9. Provision User Account if requested
        Long createdUserId = null;
        if (request.isCreateLoginAccount()) {
            createdUserId = provisionUserAccount(savedEmployee, request.getInitialPassword());
        }

        EmployeeResponse response = mapToResponse(savedEmployee);
        if (createdUserId != null) {
            response.setUserId(createdUserId);
            response.setHasLoginAccount(true);
        }
        return response;
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, UpdateEmployeeRequest request) {
        log.info("Updating employee with id: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        enforceTenantAccess(employee);

        // Validate Branch
        if (request.getBranchId() != null) {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.getBranchId()));
            if (!branch.getCompany().getId().equals(employee.getCompany().getId())) {
                throw new BadRequestException("Branch does not belong to the employee's company");
            }
            employee.setBranch(branch);
        } else {
            employee.setBranch(null);
        }

        // Validate Department
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));
            if (!department.getCompany().getId().equals(employee.getCompany().getId())) {
                throw new BadRequestException("Department does not belong to the employee's company");
            }
            employee.setDepartment(department);
        } else {
            employee.setDepartment(null);
        }

        // Validate Designation
        if (request.getDesignationId() != null) {
            Designation designation = designationRepository.findById(request.getDesignationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Designation not found with id: " + request.getDesignationId()));
            if (!designation.getCompany().getId().equals(employee.getCompany().getId())) {
                throw new BadRequestException("Designation does not belong to the employee's company");
            }
            employee.setDesignation(designation);
        } else {
            employee.setDesignation(null);
        }

        // Validate Manager
        if (request.getManagerId() != null) {
            if (request.getManagerId().equals(id)) {
                throw new BadRequestException("An employee cannot report to themselves");
            }
            Employee manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + request.getManagerId()));
            if (!manager.getCompany().getId().equals(employee.getCompany().getId())) {
                throw new BadRequestException("Manager does not belong to the employee's company");
            }

            // Check circular reporting (manager cannot directly or indirectly report to this employee)
            validateNoReportingLoop(id, manager);
            employee.setManager(manager);
        } else {
            employee.setManager(null);
        }

        employee.setFirstName(request.getFirstName().trim());
        employee.setLastName(request.getLastName().trim());
        employee.setPhoneNumber(request.getPhoneNumber());
        employee.setDateOfBirth(request.getDateOfBirth());
        employee.setGender(request.getGender());
        employee.setConfirmationDate(request.getConfirmationDate());

        Employee updated = employeeRepository.save(employee);

        // Sync name/phone with User account if exists
        userRepository.findByEmployeeId(id).ifPresent(user -> {
            user.setFirstName(updated.getFirstName());
            user.setLastName(updated.getLastName());
            user.setPhoneNumber(updated.getPhoneNumber());
            userRepository.save(user);
        });

        return mapToResponse(updated);
    }

    @Transactional
    public EmployeeResponse updateStatus(Long id, UpdateEmployeeStatusRequest request) {
        log.info("Updating status for employee id: {} to {}", id, request.getStatus());

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        enforceTenantAccess(employee);

        employee.setStatus(request.getStatus());
        Employee updated = employeeRepository.save(employee);

        // Synchronize with User account
        userRepository.findByEmployeeId(id).ifPresent(user -> {
            if (request.getStatus() == EmploymentStatus.SEPARATED || request.getStatus() == EmploymentStatus.SUSPENDED) {
                user.setActive(false);
                log.info("Deactivated user account for employee id: {} due to status: {}", id, request.getStatus());
            } else if (request.getStatus() == EmploymentStatus.ACTIVE) {
                user.setActive(true);
                log.info("Activated user account for employee id: {}", id);
            }
            userRepository.save(user);
        });

        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<EmployeeSummaryDto> getEligibleManagers(Long companyId, Long currentEmployeeId) {
        Long effectiveCompanyId = companyId;
        List<Long> excludedEmpIds = new ArrayList<>();
        if (currentEmployeeId != null) {
            excludedEmpIds.add(currentEmployeeId);
        }

        if (!SecurityUtils.isCurrentUserSuperAdmin()) {
            User currentUser = resolveCurrentUser();
            if (currentUser != null) {
                if (currentUser.getCompanyId() != null) {
                    effectiveCompanyId = currentUser.getCompanyId();
                }
                // Include eligible managers across organization
            }
        }

        return employeeRepository.findEligibleManagers(effectiveCompanyId, currentEmployeeId).stream()
                .filter(emp -> !excludedEmpIds.contains(emp.getId()))
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmployeeSummaryDto> getDirectReports(Long managerId) {
        return employeeRepository.findByManagerId(managerId).stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    // --- Private Helper Methods ---

    private String generateUniqueEmployeeCode(Company company) {
        String prefix = "EMP-";
        long nextNum = employeeRepository.countByCompanyId(company.getId()) + 1;
        String candidate = prefix + String.format("%04d", nextNum);

        int offset = 1;
        while (employeeRepository.existsByEmployeeCode(candidate)) {
            candidate = prefix + String.format("%04d", nextNum + offset);
            offset++;
        }
        return candidate;
    }

    private Long provisionUserAccount(Employee employee, String initialPassword) {
        String roleName = "ROLE_EMPLOYEE";
        String deptName = employee.getDepartment() != null ? employee.getDepartment().getName().toLowerCase() : "";
        String deptCode = employee.getDepartment() != null && employee.getDepartment().getCode() != null ? employee.getDepartment().getCode().toLowerCase() : "";
        String desName = employee.getDesignation() != null ? employee.getDesignation().getName().toLowerCase() : "";
        String desCode = employee.getDesignation() != null && employee.getDesignation().getCode() != null ? employee.getDesignation().getCode().toLowerCase() : "";

        if (deptCode.contains("hr") || deptName.contains("human resource") || deptName.contains("hr") ||
            desCode.contains("hr") || desName.contains("hr admin") || desName.contains("human resource")) {
            roleName = "ROLE_HR_ADMIN";
        } else if (desName.contains("manager") || "des-emg".equals(desCode)) {
            roleName = "ROLE_MANAGER";
        } else if (desName.contains("lead") || desName.contains("team lead") || "des-etl".equals(desCode)) {
            roleName = "ROLE_TEAM_LEAD";
        }

        Role roleToAssign = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.findByName("ROLE_EMPLOYEE")
                        .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_EMPLOYEE not found")));

        String rawPassword = StringUtils.hasText(initialPassword) ? initialPassword : "Welcome@123";

        User user = new User();
        user.setEmail(employee.getEmail());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFirstName(employee.getFirstName());
        user.setLastName(employee.getLastName());
        user.setPhoneNumber(employee.getPhoneNumber());
        user.setEmployeeId(employee.getId());
        user.setActive(employee.getStatus() == EmploymentStatus.ACTIVE || employee.getStatus() == EmploymentStatus.PROBATION);
        user.setEmailVerified(true);
        user.setRoles(Collections.singleton(roleToAssign));

        // Assign Tenant and Scope attributes
        user.setCompanyId(employee.getCompany() != null ? employee.getCompany().getId() : null);
        user.setScopeType(ScopeType.ORGANIZATION);
        user.setScopeBranchId(employee.getBranch() != null ? employee.getBranch().getId() : null);
        user.setScopeDepartmentId(employee.getDepartment() != null ? employee.getDepartment().getId() : null);

        User savedUser = userRepository.save(user);
        log.info("Provisioned new user account with id: {} and role: {} for employee: {}", savedUser.getId(), roleName, employee.getEmployeeCode());
        return savedUser.getId();
    }

    private void validateNoReportingLoop(Long employeeId, Employee managerCandidate) {
        if (managerCandidate.getId().equals(employeeId)) {
            // Allow top-level self-reporting root
            return;
        }
        Employee current = managerCandidate;
        while (current != null) {
            if (current.getId().equals(employeeId)) {
                throw new BadRequestException("Circular reporting loop detected: manager cannot report to this employee");
            }
            if (current.getManager() != null && current.getManager().getId().equals(current.getId())) {
                break;
            }
            current = current.getManager();
        }
    }

    private EmployeeResponse mapToResponse(Employee emp) {
        Optional<User> linkedUser = userRepository.findByEmployeeId(emp.getId());

        EmployeeResponse response = new EmployeeResponse();
        response.setId(emp.getId());
        response.setEmployeeCode(emp.getEmployeeCode());
        response.setFirstName(emp.getFirstName());
        response.setLastName(emp.getLastName());
        response.setFullName(emp.getFullName());
        response.setEmail(emp.getEmail());
        response.setPhoneNumber(emp.getPhoneNumber());
        response.setDateOfBirth(emp.getDateOfBirth());
        response.setGender(emp.getGender());
        response.setJoiningDate(emp.getJoiningDate());
        response.setConfirmationDate(emp.getConfirmationDate());
        response.setStatus(emp.getStatus());

        if (emp.getCompany() != null) {
            response.setCompanyId(emp.getCompany().getId());
            response.setCompanyName(emp.getCompany().getName());
        }
        if (emp.getBranch() != null) {
            response.setBranchId(emp.getBranch().getId());
            response.setBranchName(emp.getBranch().getName());
        }
        if (emp.getDepartment() != null) {
            response.setDepartmentId(emp.getDepartment().getId());
            response.setDepartmentName(emp.getDepartment().getName());
        }
        if (emp.getDesignation() != null) {
            response.setDesignationId(emp.getDesignation().getId());
            response.setDesignationName(emp.getDesignation().getName());
        }
        if (emp.getManager() != null) {
            response.setManagerId(emp.getManager().getId());
            response.setManagerName(emp.getManager().getFullName());
            response.setManagerCode(emp.getManager().getEmployeeCode());
        }

        response.setUserId(linkedUser.map(User::getId).orElse(null));
        response.setHasLoginAccount(linkedUser.isPresent());
        response.setCreatedAt(emp.getCreatedAt());
        response.setUpdatedAt(emp.getUpdatedAt());

        return response;
    }

    private EmployeeSummaryDto mapToSummary(Employee emp) {
        EmployeeSummaryDto dto = new EmployeeSummaryDto();
        dto.setId(emp.getId());
        dto.setEmployeeCode(emp.getEmployeeCode());
        dto.setFullName(emp.getFullName());
        dto.setEmail(emp.getEmail());
        if (emp.getDepartment() != null) {
            dto.setDepartmentName(emp.getDepartment().getName());
        }
        if (emp.getDesignation() != null) {
            dto.setDesignationName(emp.getDesignation().getName());
        }
        dto.setStatus(emp.getStatus());
        return dto;
    }

    private User resolveCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail().orElse(null);
        if (email == null) return null;
        return userRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    private Long resolveCurrentUserCompanyId() {
        User u = resolveCurrentUser();
        return u != null ? u.getCompanyId() : null;
    }

    private void enforceTenantAccess(Employee employee) {
        if (SecurityUtils.isCurrentUserSuperAdmin()) {
            return;
        }
        User currentUser = resolveCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("Unauthorized: No active user session");
        }
        if (employee.getCompany() != null && !currentUser.getCompanyId().equals(employee.getCompany().getId())) {
            throw new UnauthorizedException("Forbidden: Access restricted to authorized organization only");
        }
        if (currentUser.getScopeType() == ScopeType.BRANCH && currentUser.getScopeBranchId() != null) {
            if (employee.getBranch() == null || !currentUser.getScopeBranchId().equals(employee.getBranch().getId())) {
                throw new UnauthorizedException("Forbidden: Access restricted to your assigned branch location only");
            }
        }
        if (currentUser.isManager() || currentUser.isTeamLead()) {
            Long currentEmpId = currentUser.getEmployeeId();
            if (currentEmpId == null) {
                Employee self = employeeRepository.findByEmailIgnoreCase(currentUser.getEmail()).orElse(null);
                if (self != null) {
                    currentEmpId = self.getId();
                }
            }
            if (currentEmpId != null && !currentEmpId.equals(employee.getId())) {
                List<Long> subordinates = currentUser.isManager()
                        ? getSubordinateEmployeeIds(currentEmpId)
                        : getDirectReporteeIds(currentEmpId);
                if (!subordinates.contains(employee.getId())) {
                    throw new UnauthorizedException("Forbidden: Access restricted strictly to employees reporting to you");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Long> getSubordinateEmployeeIds(Long managerId) {
        if (managerId == null) {
            return Collections.emptyList();
        }
        String sql = "WITH RECURSIVE emp_tree AS (" +
                "  SELECT id FROM employees WHERE manager_id = :managerId " +
                "  UNION ALL " +
                "  SELECT e.id FROM employees e INNER JOIN emp_tree et ON e.manager_id = et.id " +
                ") SELECT id FROM emp_tree";
        List<Number> results = entityManager.createNativeQuery(sql)
                .setParameter("managerId", managerId)
                .getResultList();
        return results.stream().map(Number::longValue).collect(Collectors.toList());
    }

    private List<Long> getDirectReporteeIds(Long managerId) {
        if (managerId == null) {
            return Collections.emptyList();
        }
        return employeeRepository.findByManagerId(managerId).stream()
                .map(Employee::getId)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteEmployee(Long id) {
        deleteEmployeeByIdentifier(String.valueOf(id));
    }

    @Transactional
    public void deleteEmployeeByIdentifier(String identifier) {
        Employee employee;
        if (identifier.matches("\\d+")) {
            Long id = Long.parseLong(identifier);
            employee = employeeRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        } else {
            employee = employeeRepository.findByEmployeeCode(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "code", identifier));
        }

        Long empId = employee.getId();
        enforceTenantAccess(employee);

        // 1. Unlink reporting relationships if this employee was a manager
        entityManager.createNativeQuery("UPDATE employees SET manager_id = NULL WHERE manager_id = :id")
                .setParameter("id", empId)
                .executeUpdate();

        // 2. Unlink any approver references in leave and regularization requests
        entityManager.createNativeQuery("UPDATE regularization_requests SET approver_id = NULL WHERE approver_id = :id")
                .setParameter("id", empId)
                .executeUpdate();

        entityManager.createNativeQuery("UPDATE leave_requests SET approver_id = NULL WHERE approver_id = :id")
                .setParameter("id", empId)
                .executeUpdate();

        // 3. Unlink or remove associated User login account (preserve Super Admin, Tenant Admin and HR Admin login accounts)
        Optional<User> linkedUser = userRepository.findByEmployeeId(empId)
                .or(() -> userRepository.findByEmailIgnoreCase(employee.getEmail()));
        if (linkedUser.isPresent()) {
            User user = linkedUser.get();
            if (user.isSuperAdmin() || user.isTenantAdmin() || user.isHrAdmin()) {
                user.setEmployeeId(null);
                userRepository.save(user);
                log.info("Unlinked employee profile from administrator account: {}", user.getEmail());
            } else {
                user.getRoles().clear();
                userRepository.delete(user);
                log.info("Deprovisioned linked user account: {}", user.getEmail());
            }
        }

        // 4. Delete employee record (cascading documents, attendance, leaves, etc.)
        employeeRepository.delete(employee);
        log.info("Employee deleted permanently: ID={}, Code={}, Name={}",
                employee.getId(), employee.getEmployeeCode(), employee.getFullName());
    }
}
