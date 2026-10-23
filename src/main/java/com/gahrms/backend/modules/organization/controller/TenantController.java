package com.gahrms.backend.modules.organization.controller;

import com.gahrms.backend.common.dto.ApiResponse;
import com.gahrms.backend.common.dto.PageResponse;
import com.gahrms.backend.common.exception.UnauthorizedException;
import com.gahrms.backend.common.util.SecurityUtils;
import com.gahrms.backend.modules.identity.entity.User;
import com.gahrms.backend.modules.identity.repository.UserRepository;
import com.gahrms.backend.modules.organization.dto.CreateTenantRequest;
import com.gahrms.backend.modules.organization.dto.TenantDto;
import com.gahrms.backend.modules.organization.dto.UpdateTenantRequest;
import com.gahrms.backend.modules.organization.entity.TenantStatus;
import com.gahrms.backend.modules.organization.service.TenantService;
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
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Multi-Tenant Platform Governance", description = "Platform-level APIs for managing customer organization tenants, subscriptions, and lifecycle")
@SecurityRequirement(name = "BearerAuth")
public class TenantController {

    private final TenantService tenantService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Onboard new customer tenant organization",
            description = "Creates a customer organization (company), sets up default headquarters & core departments, and provisions the initial Tenant Admin account"
    )
    public ResponseEntity<ApiResponse<TenantDto>> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        TenantDto created = tenantService.createTenant(request);
        return ResponseEntity.ok(ApiResponse.created("Tenant organization created successfully", created));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "List all customer tenant organizations",
            description = "Paginated list of all customer organizations across the platform with employee counts, status, and search filters"
    )
    public ResponseEntity<ApiResponse<PageResponse<TenantDto>>> getTenants(
            @Parameter(description = "Keyword search by name, code, domain or contact email") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by tenant status") @RequestParam(required = false) TenantStatus status,
            @Parameter(description = "Filter by subscription plan") @RequestParam(required = false) String subscriptionPlan,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<TenantDto> result = tenantService.getTenants(search, status, subscriptionPlan, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/my-tenant")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current authenticated user's tenant organization", description = "Retrieves the organization profile for the logged in user's company")
    public ResponseEntity<ApiResponse<TenantDto>> getMyTenant() {
        String email = SecurityUtils.getCurrentUserEmail()
                .orElseThrow(() -> new UnauthorizedException("Unauthenticated"));
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (user.getCompanyId() == null) {
            throw new UnauthorizedException("Platform Super Admin has no single tenant. Access /api/v1/tenants instead.");
        }

        return ResponseEntity.ok(ApiResponse.ok(tenantService.getTenantById(user.getCompanyId())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Get tenant organization by ID", description = "Retrieve complete organization profile and operational metrics")
    public ResponseEntity<ApiResponse<TenantDto>> getTenantById(@PathVariable Long id) {
        enforceTenantAccess(id);
        return ResponseEntity.ok(ApiResponse.ok(tenantService.getTenantById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Update tenant organization profile", description = "Modify organization name, tax credentials, contacts, and operational settings")
    public ResponseEntity<ApiResponse<TenantDto>> updateTenant(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTenantRequest request
    ) {
        enforceTenantAccess(id);
        TenantDto updated = tenantService.updateTenant(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Tenant organization updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Transition tenant lifecycle status",
            description = "Activate, suspend or terminate a customer tenant organization. Suspending deactivates all linked tenant user accounts."
    )
    public ResponseEntity<ApiResponse<TenantDto>> updateTenantStatus(
            @PathVariable Long id,
            @RequestParam TenantStatus status
    ) {
        TenantDto updated = tenantService.updateTenantStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Tenant status updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Permanently delete tenant organization", description = "Permanently deletes tenant organization and cascades deletion of branches, departments, workforce, and user accounts")
    public ResponseEntity<ApiResponse<Void>> deleteTenant(@PathVariable Long id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.ok(ApiResponse.ok("Tenant organization permanently deleted successfully", null));
    }

    private void enforceTenantAccess(Long requestedCompanyId) {
        if (SecurityUtils.isCurrentUserSuperAdmin()) {
            return;
        }
        String email = SecurityUtils.getCurrentUserEmail().orElse("");
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || user.getCompanyId() == null || !user.getCompanyId().equals(requestedCompanyId)) {
            throw new UnauthorizedException("Forbidden: Access restricted to authorized organization only");
        }
    }
}
