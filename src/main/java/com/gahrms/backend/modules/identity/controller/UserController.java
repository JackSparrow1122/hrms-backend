package com.gahrms.backend.modules.identity.controller;

import com.gahrms.backend.common.dto.ApiResponse;
import com.gahrms.backend.common.dto.PageResponse;
import com.gahrms.backend.modules.identity.dto.AssignRolesRequest;
import com.gahrms.backend.modules.identity.dto.CreateUserRequest;
import com.gahrms.backend.modules.identity.dto.UpdateUserRequest;
import com.gahrms.backend.modules.identity.dto.UserDto;
import com.gahrms.backend.modules.identity.entity.ScopeType;
import com.gahrms.backend.modules.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User & Access Governance (RBAC)", description = "APIs for user provisioning, role assignments, data scope definitions, and lifecycle governance")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(
            summary = "Create user account with roles & data scope",
            description = "Provisions a new user with assigned roles (e.g. TENANT_ADMIN, HR_ADMIN, MANAGER, TEAM_LEAD, EMPLOYEE) and access scope (ORGANIZATION, BRANCH, DEPARTMENT)"
    )
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDto created = userService.createUser(request);
        return ResponseEntity.ok(ApiResponse.created("User created successfully", created));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN', 'MANAGER')")
    @Operation(
            summary = "List and filter user accounts",
            description = "Paginated list of users with search keyword, role, scope and active status filters. Automatically enforces organization tenant isolation."
    )
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> getUsers(
            @Parameter(description = "Filter by company ID (Super Admin only)") @RequestParam(required = false) Long companyId,
            @Parameter(description = "Keyword search across name and email") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by role name (e.g. ROLE_HR_ADMIN, ROLE_MANAGER)") @RequestParam(required = false) String role,
            @Parameter(description = "Filter by data access scope") @RequestParam(required = false) ScopeType scopeType,
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<UserDto> result = userService.getUsers(companyId, search, role, scopeType, active, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN', 'MANAGER')")
    @Operation(summary = "Get user account details by ID", description = "Retrieve full profile, assigned roles and data access scopes")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Update user personal profile details", description = "Modify user name and phone contact")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UserDto updated = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.ok("User updated successfully", updated));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(
            summary = "Assign roles and data access scope",
            description = "Delegates organization roles (e.g. HR_ADMIN, MANAGER, TEAM_LEAD) and configures data boundary scope (ORGANIZATION, BRANCH, DEPARTMENT)"
    )
    public ResponseEntity<ApiResponse<UserDto>> assignRoles(
            @PathVariable Long id,
            @Valid @RequestBody AssignRolesRequest request
    ) {
        UserDto updated = userService.assignRolesAndScope(id, request);
        return ResponseEntity.ok(ApiResponse.ok("User roles and access scope updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Activate or deactivate user account", description = "Controls user login capability without deleting operational records")
    public ResponseEntity<ApiResponse<UserDto>> updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        UserDto updated = userService.updateUserStatus(id, active);
        return ResponseEntity.ok(ApiResponse.ok("User status updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Delete user account", description = "Permanently deletes user login profile")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User deleted successfully", null));
    }
}
