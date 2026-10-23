package com.gahrms.backend.modules.organization.service;

import com.gahrms.backend.common.exception.ConflictException;
import com.gahrms.backend.common.exception.ResourceNotFoundException;
import com.gahrms.backend.common.exception.UnauthorizedException;
import com.gahrms.backend.common.util.SecurityUtils;
import com.gahrms.backend.modules.identity.entity.Role;
import com.gahrms.backend.modules.identity.entity.ScopeType;
import com.gahrms.backend.modules.identity.entity.User;
import com.gahrms.backend.modules.identity.repository.RoleRepository;
import com.gahrms.backend.modules.identity.repository.UserRepository;
import com.gahrms.backend.modules.organization.dto.*;
import com.gahrms.backend.modules.organization.entity.Branch;
import com.gahrms.backend.modules.organization.entity.Company;
import com.gahrms.backend.modules.organization.entity.Department;
import com.gahrms.backend.modules.organization.entity.Designation;
import com.gahrms.backend.modules.employee.entity.Employee;
import com.gahrms.backend.modules.employee.entity.EmploymentStatus;
import com.gahrms.backend.modules.employee.repository.EmployeeRepository;
import com.gahrms.backend.modules.organization.repository.BranchRepository;
import com.gahrms.backend.modules.organization.repository.CompanyRepository;
import com.gahrms.backend.modules.organization.repository.DepartmentRepository;
import com.gahrms.backend.modules.organization.repository.DesignationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // --- Companies ---
    @Transactional(readOnly = true)
    public List<CompanyDto> getAllCompanies() {
        return companyRepository.findAll().stream()
                .map(this::mapToCompanyDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompanyDto getCompanyById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", id));
        return mapToCompanyDto(company);
    }

    // --- Branches ---
    @Transactional(readOnly = true)
    public List<BranchDto> getBranchesByCompany(Long companyId) {
        verifyCompanyExists(companyId);
        if (!SecurityUtils.isCurrentUserSuperAdmin()) {
            User currentUser = resolveCurrentUser();
            if (currentUser != null && currentUser.getScopeType() == ScopeType.BRANCH && currentUser.getScopeBranchId() != null) {
                return branchRepository.findById(currentUser.getScopeBranchId())
                        .map(this::mapToBranchDto)
                        .map(List::of)
                        .orElse(Collections.emptyList());
            }
        }
        return branchRepository.findByCompanyId(companyId).stream()
                .map(this::mapToBranchDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BranchDto getBranchById(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", id));
        return mapToBranchDto(branch);
    }

    @Transactional
    public BranchDto createBranch(CreateBranchRequest request) {
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", request.getCompanyId()));

        // 1. Enforce branch limit set by Super Admin
        int maxAllowed = company.getMaxBranches() != null ? company.getMaxBranches() : 5;
        long currentBranches = branchRepository.countByCompanyId(company.getId());
        if (currentBranches >= maxAllowed) {
            throw new ConflictException("Branch quota reached (" + currentBranches + "/" + maxAllowed + 
                    "). Super Admin has set a limit of " + maxAllowed + " branches for " + company.getName() + 
                    ". Please contact Super Admin to increase your branch limit.");
        }

        if (branchRepository.existsByCompanyIdAndCode(request.getCompanyId(), request.getCode())) {
            throw new ConflictException("Branch with code '" + request.getCode() + "' already exists for this company");
        }

        // 2. Validate HR Admin email if supplied
        if (StringUtils.hasText(request.getHrAdminEmail())) {
            String hrEmail = request.getHrAdminEmail().trim().toLowerCase();
            if (userRepository.existsByEmailIgnoreCase(hrEmail)) {
                throw new ConflictException("A user account with email '" + hrEmail + "' already exists");
            }
        }

        Branch branch = new Branch();
        branch.setCompany(company);
        branch.setName(request.getName().trim());
        branch.setCode(request.getCode().trim().toUpperCase());
        branch.setCity(request.getCity());
        branch.setState(request.getState());
        branch.setCountry(request.getCountry() != null ? request.getCountry() : "India");
        branch.setHeadquarters(request.isHeadquarters());

        Branch savedBranch = branchRepository.save(branch);

        // 3. Provision dedicated Branch HR Admin if specified
        if (StringUtils.hasText(request.getHrAdminEmail())) {
            Role hrRole = roleRepository.findByName("ROLE_HR_ADMIN")
                    .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_HR_ADMIN not found"));

            String rawPassword = StringUtils.hasText(request.getHrAdminPassword()) ? request.getHrAdminPassword().trim() : "Welcome@123";
            String hrFirstName = StringUtils.hasText(request.getHrAdminFirstName()) ? request.getHrAdminFirstName().trim() : "Branch";
            String hrLastName = StringUtils.hasText(request.getHrAdminLastName()) ? request.getHrAdminLastName().trim() : "HR Admin";
            String hrEmail = request.getHrAdminEmail().trim().toLowerCase();

            // Find or resolve HR Department
            Department hrDept = departmentRepository.findByCompanyId(company.getId()).stream()
                    .filter(d -> d.getName().toLowerCase().contains("human") || d.getName().toLowerCase().contains("hr"))
                    .findFirst()
                    .orElse(null);

            // Find or resolve HR Designation
            Designation hrDesig = designationRepository.findByCompanyId(company.getId()).stream()
                    .filter(d -> d.getName().toLowerCase().contains("director") || d.getName().toLowerCase().contains("hr"))
                    .findFirst()
                    .orElse(null);

            // Find Head HR of the company to set as reporting manager
            Employee headHrEmp = employeeRepository.findByCompanyId(company.getId()).stream()
                    .filter(e -> e.getBranch() == null || (e.getEmployeeCode() != null && e.getEmployeeCode().endsWith("-0001")))
                    .findFirst()
                    .orElse(null);

            // Generate unique employee code
            long empCount = employeeRepository.countByCompanyId(company.getId()) + 1;
            String empCode = String.format("EMP-%04d", empCount);
            while (employeeRepository.existsByEmployeeCode(empCode)) {
                empCount++;
                empCode = String.format("EMP-%04d", empCount);
            }

            // Create master Employee record for Branch HR Admin
            Employee hrEmployee = Employee.builder()
                    .company(company)
                    .branch(savedBranch)
                    .department(hrDept)
                    .designation(hrDesig)
                    .manager(headHrEmp)
                    .employeeCode(empCode)
                    .firstName(hrFirstName)
                    .lastName(hrLastName)
                    .email(hrEmail)
                    .phoneNumber(request.getHrAdminPhone())
                    .status(EmploymentStatus.ACTIVE)
                    .joiningDate(LocalDate.now())
                    .build();

            Employee savedHrEmployee = employeeRepository.save(hrEmployee);

            // Create User login account for Branch HR Admin
            User hrUser = new User();
            hrUser.setEmail(hrEmail);
            hrUser.setPassword(passwordEncoder.encode(rawPassword));
            hrUser.setFirstName(hrFirstName);
            hrUser.setLastName(hrLastName);
            hrUser.setPhoneNumber(request.getHrAdminPhone());
            hrUser.setCompanyId(company.getId());
            hrUser.setEmployeeId(savedHrEmployee.getId());
            hrUser.setScopeType(ScopeType.BRANCH);
            hrUser.setScopeBranchId(savedBranch.getId());
            hrUser.setActive(true);
            hrUser.setEmailVerified(true);
            hrUser.setRoles(new HashSet<>(Collections.singletonList(hrRole)));

            userRepository.save(hrUser);
            log.info("Provisioned Branch HR Admin user: {} (Emp ID: {}) for branch: {} (Company: {})",
                    hrUser.getEmail(), savedHrEmployee.getId(), savedBranch.getName(), company.getName());
        }

        return mapToBranchDto(savedBranch);
    }

    @Transactional
    public BranchDto updateBranch(Long id, UpdateBranchRequest request) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", id));

        if (request.getName() != null && !request.getName().isBlank()) {
            branch.setName(request.getName().trim());
        }
        if (request.getCity() != null) {
            branch.setCity(request.getCity());
        }
        if (request.getState() != null) {
            branch.setState(request.getState());
        }
        if (request.getCountry() != null) {
            branch.setCountry(request.getCountry());
        }
        if (request.getHeadquarters() != null) {
            branch.setHeadquarters(request.getHeadquarters());
        }
        branch.setUpdatedAt(Instant.now());

        return mapToBranchDto(branchRepository.save(branch));
    }

    @Transactional
    public void deleteBranch(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", id));

        if (branch.isHeadquarters()) {
            throw new ConflictException("Headquarters branch cannot be deleted.");
        }

        long employeeCount = employeeRepository.countByBranchId(id);
        if (employeeCount > 0) {
            throw new ConflictException("Cannot delete branch '" + branch.getName() + "'. It currently has " + employeeCount + " active workforce members assigned.");
        }

        // Unlink any users scoped to this branch
        List<User> scopedUsers = userRepository.findByCompanyIdAndScopeBranchId(branch.getCompany().getId(), id);
        for (User u : scopedUsers) {
            u.setScopeBranchId(null);
            userRepository.save(u);
        }

        branchRepository.delete(branch);
        log.info("Branch #{} ({}) successfully deleted.", id, branch.getName());
    }

    @Transactional
    public BranchDto assignBranchHrAdmin(Long branchId, AssignBranchHrRequest request) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", branchId));

        Role hrRole = roleRepository.findByName("ROLE_HR_ADMIN")
                .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_HR_ADMIN not found"));

        String email = request.getEmail().trim().toLowerCase();
        Optional<User> existingUserOpt = userRepository.findByEmailIgnoreCase(email);

        User hrUser;
        if (existingUserOpt.isPresent()) {
            hrUser = existingUserOpt.get();
            if (!hrUser.getCompanyId().equals(branch.getCompany().getId())) {
                throw new ConflictException("The user account '" + email + "' belongs to a different organization.");
            }
            hrUser.setFirstName(request.getFirstName().trim());
            hrUser.setLastName(request.getLastName().trim());
            hrUser.setScopeType(ScopeType.BRANCH);
            hrUser.setScopeBranchId(branch.getId());
            hrUser.getRoles().add(hrRole);
            if (StringUtils.hasText(request.getPassword())) {
                hrUser.setPassword(passwordEncoder.encode(request.getPassword().trim()));
            }
            if (StringUtils.hasText(request.getPhoneNumber())) {
                hrUser.setPhoneNumber(request.getPhoneNumber().trim());
            }
        } else {
            hrUser = new User();
            hrUser.setEmail(email);
            hrUser.setPassword(passwordEncoder.encode(StringUtils.hasText(request.getPassword()) ? request.getPassword().trim() : "Welcome@123"));
            hrUser.setFirstName(request.getFirstName().trim());
            hrUser.setLastName(request.getLastName().trim());
            hrUser.setPhoneNumber(request.getPhoneNumber());
            hrUser.setCompanyId(branch.getCompany().getId());
            hrUser.setScopeType(ScopeType.BRANCH);
            hrUser.setScopeBranchId(branch.getId());
            hrUser.setActive(true);
            hrUser.setEmailVerified(true);
            hrUser.setRoles(new HashSet<>(Collections.singletonList(hrRole)));
        }

        userRepository.save(hrUser);
        log.info("Assigned Branch HR Admin {} to branch #{} ({})", hrUser.getEmail(), branch.getId(), branch.getName());

        return mapToBranchDto(branch);
    }

    // --- Departments ---
    @Transactional(readOnly = true)
    public List<DepartmentDto> getDepartmentsByCompany(Long companyId) {
        return getDepartmentsByCompany(companyId, null);
    }

    @Transactional(readOnly = true)
    public List<DepartmentDto> getDepartmentsByCompany(Long companyId, Long branchId) {
        Long effectiveCompanyId = companyId;

        if (!SecurityUtils.isCurrentUserSuperAdmin()) {
            User currentUser = resolveCurrentUser();
            if (currentUser != null && currentUser.getCompanyId() != null) {
                effectiveCompanyId = currentUser.getCompanyId();
            }
        }

        verifyCompanyExists(effectiveCompanyId);

        // Departments are organization-wide catalogs shared across all multiple branches of the company
        List<Department> departments = departmentRepository.findByCompanyId(effectiveCompanyId);

        return departments.stream()
                .map(this::mapToDepartmentDto)
                .toList();
    }

    @Transactional
    public DepartmentDto createDepartment(CreateDepartmentRequest request) {
        Long effectiveCompanyId = request.getCompanyId();
        Long effectiveBranchId = request.getBranchId();

        if (!SecurityUtils.isCurrentUserSuperAdmin()) {
            User currentUser = resolveCurrentUser();
            if (currentUser != null) {
                if (currentUser.getCompanyId() != null) {
                    effectiveCompanyId = currentUser.getCompanyId();
                }
                if (currentUser.getScopeType() == ScopeType.BRANCH && currentUser.getScopeBranchId() != null) {
                    effectiveBranchId = currentUser.getScopeBranchId();
                }
            }
        }

        final Long compId = effectiveCompanyId;
        final Long brId = effectiveBranchId;

        Company company = companyRepository.findById(compId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", compId));

        if (departmentRepository.existsByCompanyIdAndCode(compId, request.getCode())) {
            throw new ConflictException("Department with code '" + request.getCode() + "' already exists for this company");
        }

        Department parentDept = null;
        if (request.getParentDepartmentId() != null) {
            parentDept = departmentRepository.findById(request.getParentDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Department", "id", request.getParentDepartmentId()));
        }

        Department department = new Department();
        department.setCompany(company);
        department.setParentDepartment(parentDept);
        department.setName(request.getName());
        department.setCode(request.getCode());
        department.setActive(true);

        if (brId != null) {
            Branch branch = branchRepository.findById(brId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", brId));
            department.setBranch(branch);
        }

        return mapToDepartmentDto(departmentRepository.save(department));
    }

    @Transactional(readOnly = true)
    public DepartmentDto getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        return mapToDepartmentDto(department);
    }

    @Transactional
    public DepartmentDto updateDepartment(Long id, UpdateDepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        if (!SecurityUtils.isCurrentUserSuperAdmin()) {
            User currentUser = resolveCurrentUser();
            if (currentUser != null && currentUser.getScopeType() == ScopeType.BRANCH && currentUser.getScopeBranchId() != null) {
                if (department.getBranch() != null && !department.getBranch().getId().equals(currentUser.getScopeBranchId())) {
                    throw new UnauthorizedException("Forbidden: Access restricted to your assigned branch location only");
                }
            }
        }

        if (request.getCode() != null && !request.getCode().isBlank() && !request.getCode().equalsIgnoreCase(department.getCode())) {
            if (departmentRepository.existsByCompanyIdAndCode(department.getCompany().getId(), request.getCode())) {
                throw new ConflictException("Department with code '" + request.getCode() + "' already exists for this company");
            }
            department.setCode(request.getCode().toUpperCase().trim());
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            department.setName(request.getName().trim());
        }

        if (request.getBranchId() != null) {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", request.getBranchId()));
            department.setBranch(branch);
        }

        if (request.getParentDepartmentId() != null) {
            if (request.getParentDepartmentId().equals(id)) {
                throw new ConflictException("Department cannot be its own parent hierarchy");
            }
            Department parent = departmentRepository.findById(request.getParentDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Department", "id", request.getParentDepartmentId()));
            department.setParentDepartment(parent);
        }

        if (request.getActive() != null) {
            department.setActive(request.getActive());
        }

        department.setUpdatedAt(Instant.now());
        return mapToDepartmentDto(departmentRepository.save(department));
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        if (!SecurityUtils.isCurrentUserSuperAdmin()) {
            User currentUser = resolveCurrentUser();
            if (currentUser != null && currentUser.getScopeType() == ScopeType.BRANCH && currentUser.getScopeBranchId() != null) {
                if (department.getBranch() != null && !department.getBranch().getId().equals(currentUser.getScopeBranchId())) {
                    throw new UnauthorizedException("Forbidden: Access restricted to your assigned branch location only");
                }
            }
        }

        long employeeCount = employeeRepository.countByDepartmentId(id);
        if (employeeCount > 0) {
            throw new ConflictException("Cannot delete department '" + department.getName() + "'. It currently has " + employeeCount + " active employees assigned.");
        }

        if (departmentRepository.existsByParentDepartmentId(id)) {
            throw new ConflictException("Cannot delete department '" + department.getName() + "' because child sub-departments depend on it.");
        }

        departmentRepository.delete(department);
        log.info("Department #{} ({}) successfully deleted.", id, department.getName());
    }

    // --- Designations ---
    @Transactional(readOnly = true)
    public List<DesignationDto> getDesignationsByCompany(Long companyId) {
        return getDesignationsByCompany(companyId, null, null);
    }

    @Transactional(readOnly = true)
    public List<DesignationDto> getDesignationsByCompany(Long companyId, Long departmentId) {
        return getDesignationsByCompany(companyId, departmentId, null);
    }

    @Transactional(readOnly = true)
    public List<DesignationDto> getDesignationsByCompany(Long companyId, Long departmentId, Long branchId) {
        Long effectiveCompanyId = companyId;

        if (!SecurityUtils.isCurrentUserSuperAdmin()) {
            User currentUser = resolveCurrentUser();
            if (currentUser != null && currentUser.getCompanyId() != null) {
                effectiveCompanyId = currentUser.getCompanyId();
            }
        }

        verifyCompanyExists(effectiveCompanyId);

        // Designations are organization-wide catalogs shared across all multiple branches of the company
        List<Designation> designations;
        if (departmentId != null) {
            designations = designationRepository.findByCompanyIdAndDepartmentIdAndActiveTrueOrderByLevelDesc(effectiveCompanyId, departmentId);
        } else {
            designations = designationRepository.findByCompanyIdAndActiveTrueOrderByLevelDesc(effectiveCompanyId);
        }

        return designations.stream()
                .map(this::mapToDesignationDto)
                .toList();
    }

    @Transactional
    public DesignationDto createDesignation(CreateDesignationRequest request) {
        Long effectiveCompanyId = request.getCompanyId();
        Long effectiveBranchId = request.getBranchId();

        if (!SecurityUtils.isCurrentUserSuperAdmin()) {
            User currentUser = resolveCurrentUser();
            if (currentUser != null) {
                if (currentUser.getCompanyId() != null) {
                    effectiveCompanyId = currentUser.getCompanyId();
                }
                if (currentUser.getScopeType() == ScopeType.BRANCH && currentUser.getScopeBranchId() != null) {
                    effectiveBranchId = currentUser.getScopeBranchId();
                }
            }
        }

        final Long compId = effectiveCompanyId;
        final Long brId = effectiveBranchId;

        Company company = companyRepository.findById(compId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", compId));

        if (designationRepository.existsByCompanyIdAndCode(compId, request.getCode())) {
            throw new ConflictException("Designation with code '" + request.getCode() + "' already exists for this company");
        }

        Designation designation = new Designation();
        designation.setCompany(company);
        designation.setName(request.getName());
        designation.setCode(request.getCode());
        designation.setLevel(request.getLevel() != null ? request.getLevel() : 1);
        designation.setActive(true);

        if (brId != null) {
            Branch branch = branchRepository.findById(brId)
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", brId));
            designation.setBranch(branch);
        }

        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
            designation.setDepartment(dept);
        }

        return mapToDesignationDto(designationRepository.save(designation));
    }

    @Transactional(readOnly = true)
    public DesignationDto getDesignationById(Long id) {
        Designation designation = designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", id));
        return mapToDesignationDto(designation);
    }

    @Transactional
    public DesignationDto updateDesignation(Long id, UpdateDesignationRequest request) {
        Designation designation = designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", id));

        if (!SecurityUtils.isCurrentUserSuperAdmin()) {
            User currentUser = resolveCurrentUser();
            if (currentUser != null && currentUser.getScopeType() == ScopeType.BRANCH && currentUser.getScopeBranchId() != null) {
                if (designation.getBranch() != null && !designation.getBranch().getId().equals(currentUser.getScopeBranchId())) {
                    throw new UnauthorizedException("Forbidden: Access restricted to your assigned branch location only");
                }
            }
        }

        if (!designation.getCode().equalsIgnoreCase(request.getCode())) {
            if (designationRepository.existsByCompanyIdAndCode(designation.getCompany().getId(), request.getCode())) {
                throw new ConflictException("Designation with code '" + request.getCode() + "' already exists for this company");
            }
        }

        designation.setName(request.getName());
        designation.setCode(request.getCode());
        if (request.getLevel() != null) {
            designation.setLevel(request.getLevel());
        }
        if (request.getActive() != null) {
            designation.setActive(request.getActive());
        }
        if (request.getBranchId() != null) {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", request.getBranchId()));
            designation.setBranch(branch);
        }
        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
            designation.setDepartment(dept);
        }
        designation.setUpdatedAt(Instant.now());

        return mapToDesignationDto(designationRepository.save(designation));
    }

    @Transactional
    public void deleteDesignation(Long id) {
        Designation designation = designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", id));

        if (!SecurityUtils.isCurrentUserSuperAdmin()) {
            User currentUser = resolveCurrentUser();
            if (currentUser != null && currentUser.getScopeType() == ScopeType.BRANCH && currentUser.getScopeBranchId() != null) {
                if (designation.getBranch() != null && !designation.getBranch().getId().equals(currentUser.getScopeBranchId())) {
                    throw new UnauthorizedException("Forbidden: Access restricted to your assigned branch location only");
                }
            }
        }

        long employeeCount = employeeRepository.countByDesignationId(id);
        if (employeeCount > 0) {
            throw new ConflictException("Cannot delete designation '" + designation.getName() + "'. It currently has " + employeeCount + " active employees assigned.");
        }

        designationRepository.delete(designation);
        log.info("Designation #{} ({}) successfully deleted.", id, designation.getName());
    }

    // --- Helper Mappers & Validators ---
    private void verifyCompanyExists(Long companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new ResourceNotFoundException("Company", "id", companyId);
        }
    }

    private CompanyDto mapToCompanyDto(Company c) {
        CompanyDto dto = new CompanyDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setCode(c.getCode());
        dto.setRegistrationNumber(c.getRegistrationNumber());
        dto.setTaxId(c.getTaxId());
        dto.setTimezone(c.getTimezone());
        dto.setCurrency(c.getCurrency());
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }

    private BranchDto mapToBranchDto(Branch b) {
        BranchDto dto = new BranchDto();
        dto.setId(b.getId());
        dto.setCompanyId(b.getCompany().getId());
        dto.setName(b.getName());
        dto.setCode(b.getCode());
        dto.setCity(b.getCity());
        dto.setState(b.getState());
        dto.setCountry(b.getCountry());
        dto.setHeadquarters(b.isHeadquarters());
        dto.setCreatedAt(b.getCreatedAt());

        // Employee count in this branch
        dto.setEmployeeCount(employeeRepository.countByBranchId(b.getId()));

        // Branch HR Admin
        List<User> branchUsers = userRepository.findByCompanyIdAndScopeBranchId(b.getCompany().getId(), b.getId());
        branchUsers.stream()
                .filter(User::isHrAdmin)
                .findFirst()
                .ifPresent(u -> {
                    dto.setHrAdminId(u.getId());
                    dto.setHrAdminName(u.getFullName());
                    dto.setHrAdminEmail(u.getEmail());
                });

        return dto;
    }

    private DepartmentDto mapToDepartmentDto(Department d) {
        DepartmentDto dto = new DepartmentDto();
        dto.setId(d.getId());
        dto.setCompanyId(d.getCompany() != null ? d.getCompany().getId() : null);
        if (d.getBranch() != null) {
            dto.setBranchId(d.getBranch().getId());
            dto.setBranchName(d.getBranch().getName());
        }
        if (d.getParentDepartment() != null) {
            dto.setParentDepartmentId(d.getParentDepartment().getId());
            dto.setParentDepartmentName(d.getParentDepartment().getName());
        }
        dto.setName(d.getName());
        dto.setCode(d.getCode());
        dto.setActive(d.isActive());
        dto.setCreatedAt(d.getCreatedAt());
        return dto;
    }

    private DesignationDto mapToDesignationDto(Designation d) {
        DesignationDto dto = new DesignationDto();
        dto.setId(d.getId());
        dto.setCompanyId(d.getCompany() != null ? d.getCompany().getId() : null);
        if (d.getBranch() != null) {
            dto.setBranchId(d.getBranch().getId());
            dto.setBranchName(d.getBranch().getName());
        }
        dto.setName(d.getName());
        dto.setCode(d.getCode());
        dto.setLevel(d.getLevel());
        dto.setActive(d.isActive());
        if (d.getDepartment() != null) {
            dto.setDepartmentId(d.getDepartment().getId());
            dto.setDepartmentName(d.getDepartment().getName());
        }
        dto.setCreatedAt(d.getCreatedAt());
        return dto;
    }

    private User resolveCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail().orElse(null);
        if (email == null) return null;
        return userRepository.findByEmailIgnoreCase(email).orElse(null);
    }
}
