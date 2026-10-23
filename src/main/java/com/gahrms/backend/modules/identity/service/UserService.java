package com.gahrms.backend.modules.identity.service;

import com.gahrms.backend.common.dto.PageResponse;
import com.gahrms.backend.common.exception.BadRequestException;
import com.gahrms.backend.common.exception.ConflictException;
import com.gahrms.backend.common.exception.ResourceNotFoundException;
import com.gahrms.backend.common.exception.UnauthorizedException;
import com.gahrms.backend.common.util.SecurityUtils;
import com.gahrms.backend.modules.employee.entity.Employee;
import com.gahrms.backend.modules.employee.repository.EmployeeRepository;
import com.gahrms.backend.modules.identity.dto.*;
import com.gahrms.backend.modules.identity.entity.Role;
import com.gahrms.backend.modules.identity.entity.ScopeType;
import com.gahrms.backend.modules.identity.entity.User;
import com.gahrms.backend.modules.identity.repository.RoleRepository;
import com.gahrms.backend.modules.identity.repository.UserRepository;
import com.gahrms.backend.modules.identity.repository.UserSpecification;
import com.gahrms.backend.modules.organization.entity.Branch;
import com.gahrms.backend.modules.organization.entity.Company;
import com.gahrms.backend.modules.organization.entity.Department;
import com.gahrms.backend.modules.organization.repository.BranchRepository;
import com.gahrms.backend.modules.organization.repository.CompanyRepository;
import com.gahrms.backend.modules.organization.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        log.info("Creating user with email: {}", request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ConflictException("A user already exists with email: " + request.getEmail());
        }

        // Server-side tenant isolation check
        Long effectiveCompanyId = resolveCompanyScope(request.getCompanyId());

        Company company = null;
        if (effectiveCompanyId != null) {
            company = companyRepository.findById(effectiveCompanyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + effectiveCompanyId));
        }

        // Validate Branch & Department scope if specified
        if (request.getScopeBranchId() != null) {
            Branch branch = branchRepository.findById(request.getScopeBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.getScopeBranchId()));
            if (company != null && !branch.getCompany().getId().equals(company.getId())) {
                throw new BadRequestException("Scope branch does not belong to user's company");
            }
        }

        if (request.getScopeDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getScopeDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getScopeDepartmentId()));
            if (company != null && !dept.getCompany().getId().equals(company.getId())) {
                throw new BadRequestException("Scope department does not belong to user's company");
            }
        }

        Set<Role> roles = resolveRoles(request.getRoles());

        String rawPassword = StringUtils.hasText(request.getPassword()) ? request.getPassword().trim() : "Welcome@123";

        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setCompanyId(effectiveCompanyId);
        user.setScopeType(request.getScopeType() != null ? request.getScopeType() : ScopeType.ORGANIZATION);
        user.setScopeBranchId(request.getScopeBranchId());
        user.setScopeDepartmentId(request.getScopeDepartmentId());
        user.setEmployeeId(request.getEmployeeId());
        user.setActive(true);
        user.setEmailVerified(true);
        user.setRoles(roles);

        User saved = userRepository.save(user);
        log.info("User created successfully with id: {}", saved.getId());
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDto> getUsers(
            Long requestedCompanyId,
            String search,
            String roleName,
            ScopeType scopeType,
            Boolean active,
            Pageable pageable
    ) {
        Long effectiveCompanyId = resolveCompanyScope(requestedCompanyId);

        Specification<User> spec = UserSpecification.filter(
                effectiveCompanyId, search, roleName, scopeType, active
        );

        Page<User> page = userRepository.findAll(spec, pageable);
        List<UserDto> dtos = page.getContent().stream()
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
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Enforce tenant boundary
        enforceTenantAccess(user.getCompanyId());

        return mapToDto(user);
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        enforceTenantAccess(user.getCompanyId());

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPhoneNumber(request.getPhoneNumber());

        User updated = userRepository.save(user);
        return mapToDto(updated);
    }

    @Transactional
    public UserDto assignRolesAndScope(Long id, AssignRolesRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        enforceTenantAccess(user.getCompanyId());

        Set<Role> roles = resolveRoles(request.getRoles());
        user.setRoles(roles);

        if (request.getScopeType() != null) {
            user.setScopeType(request.getScopeType());
        }
        user.setScopeBranchId(request.getScopeBranchId());
        user.setScopeDepartmentId(request.getScopeDepartmentId());

        if (SecurityUtils.isCurrentUserSuperAdmin() && request.getCompanyId() != null) {
            user.setCompanyId(request.getCompanyId());
        }

        User updated = userRepository.save(user);
        log.info("Assigned roles {} and scope {} to user id: {}", request.getRoles(), user.getScopeType(), id);
        return mapToDto(updated);
    }

    @Transactional
    public UserDto updateUserStatus(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        enforceTenantAccess(user.getCompanyId());
        user.setActive(active);

        User updated = userRepository.save(user);
        log.info("User id: {} status changed to active: {}", id, active);
        return mapToDto(updated);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        enforceTenantAccess(user.getCompanyId());
        userRepository.delete(user);
        log.info("Deleted user with id: {}", id);
    }

    // --- Private Helper Methods ---

    private Long resolveCompanyScope(Long requestedCompanyId) {
        if (SecurityUtils.isCurrentUserSuperAdmin()) {
            return requestedCompanyId;
        }

        // Non-super admin is locked to their own company
        String currentUserEmail = SecurityUtils.getCurrentUserEmail()
                .orElseThrow(() -> new UnauthorizedException("Unauthenticated user"));
        User currentUser = userRepository.findByEmailIgnoreCase(currentUserEmail)
                .orElseThrow(() -> new UnauthorizedException("Current user not found"));

        if (currentUser.getCompanyId() == null) {
            throw new UnauthorizedException("User has no assigned organization/company");
        }

        if (requestedCompanyId != null && !requestedCompanyId.equals(currentUser.getCompanyId())) {
            throw new UnauthorizedException("Forbidden: Cannot access or assign across different organizations");
        }

        return currentUser.getCompanyId();
    }

    private void enforceTenantAccess(Long targetCompanyId) {
        if (SecurityUtils.isCurrentUserSuperAdmin()) {
            return;
        }
        String currentUserEmail = SecurityUtils.getCurrentUserEmail().orElse("");
        User currentUser = userRepository.findByEmailIgnoreCase(currentUserEmail).orElse(null);
        if (currentUser == null || currentUser.getCompanyId() == null || !currentUser.getCompanyId().equals(targetCompanyId)) {
            throw new UnauthorizedException("Forbidden: Access restricted to authorized organization only");
        }
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        Set<Role> roles = new HashSet<>();
        for (String rName : roleNames) {
            String formatted = rName.startsWith("ROLE_") ? rName.toUpperCase() : "ROLE_" + rName.toUpperCase();
            Role role = roleRepository.findByName(formatted)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + formatted));
            roles.add(role);
        }
        return roles;
    }

    private UserDto mapToDto(User user) {
        String companyName = null;
        if (user.getCompanyId() != null) {
            companyName = companyRepository.findById(user.getCompanyId())
                    .map(Company::getName)
                    .orElse(null);
        }

        String employeeCode = null;
        if (user.getEmployeeId() != null) {
            employeeCode = employeeRepository.findById(user.getEmployeeId())
                    .map(Employee::getEmployeeCode)
                    .orElse(null);
        }

        String branchName = null;
        if (user.getScopeBranchId() != null) {
            branchName = branchRepository.findById(user.getScopeBranchId())
                    .map(Branch::getName)
                    .orElse(null);
        }

        String departmentName = null;
        if (user.getScopeDepartmentId() != null) {
            departmentName = departmentRepository.findById(user.getScopeDepartmentId())
                    .map(Department::getName)
                    .orElse(null);
        }

        return UserDto.builder()
                .id(user.getId())
                .employeeId(user.getEmployeeId())
                .employeeCode(employeeCode)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .active(user.isActive())
                .emailVerified(user.isEmailVerified())
                .companyId(user.getCompanyId())
                .companyName(companyName)
                .scopeType(user.getScopeType())
                .scopeBranchId(user.getScopeBranchId())
                .scopeBranchName(branchName)
                .scopeDepartmentId(user.getScopeDepartmentId())
                .scopeDepartmentName(departmentName)
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
