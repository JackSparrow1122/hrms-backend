package com.gahrms.backend.modules.organization.controller;

import com.gahrms.backend.common.dto.ApiResponse;
import com.gahrms.backend.modules.organization.dto.CreateDesignationRequest;
import com.gahrms.backend.modules.organization.dto.DesignationDto;
import com.gahrms.backend.modules.organization.dto.UpdateDesignationRequest;
import com.gahrms.backend.modules.organization.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/designations")
@RequiredArgsConstructor
@Tag(name = "Designations", description = "Endpoints for managing company designations, roles, and career hierarchy levels")
@SecurityRequirement(name = "BearerAuth")
public class DesignationController {

    private final OrganizationService organizationService;

    @GetMapping
    @Operation(summary = "List designations by company", description = "Retrieve all designations ordered by seniority level, optionally filtered by department and branch")
    public ResponseEntity<ApiResponse<List<DesignationDto>>> getDesignations(
            @RequestParam(required = false, defaultValue = "1") Long companyId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.getDesignationsByCompany(companyId, departmentId, branchId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get designation by ID", description = "Retrieve detailed information for a specific designation")
    public ResponseEntity<ApiResponse<DesignationDto>> getDesignationById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.getDesignationById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Create designation", description = "Add a new designation for the company")
    public ResponseEntity<ApiResponse<DesignationDto>> createDesignation(
            @Valid @RequestBody CreateDesignationRequest request
    ) {
        DesignationDto created = organizationService.createDesignation(request);
        return ResponseEntity.ok(ApiResponse.created("Designation created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Update designation", description = "Modify an existing designation")
    public ResponseEntity<ApiResponse<DesignationDto>> updateDesignation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDesignationRequest request
    ) {
        DesignationDto updated = organizationService.updateDesignation(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Designation updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Delete designation", description = "Delete a designation if no employees are assigned to it")
    public ResponseEntity<ApiResponse<Void>> deleteDesignation(@PathVariable Long id) {
        organizationService.deleteDesignation(id);
        return ResponseEntity.ok(ApiResponse.ok("Designation deleted successfully", null));
    }
}
