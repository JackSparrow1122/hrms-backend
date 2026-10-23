package com.gahrms.backend.modules.organization.controller;

import com.gahrms.backend.common.dto.ApiResponse;
import com.gahrms.backend.modules.organization.dto.CreateDepartmentRequest;
import com.gahrms.backend.modules.organization.dto.DepartmentDto;
import com.gahrms.backend.modules.organization.dto.UpdateDepartmentRequest;
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
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "Endpoints for managing company departments and functional divisions")
@SecurityRequirement(name = "BearerAuth")
public class DepartmentController {

    private final OrganizationService organizationService;

    @GetMapping
    @Operation(summary = "List departments by company", description = "Retrieve all departments for the specified company, optionally filtered by branch")
    public ResponseEntity<ApiResponse<List<DepartmentDto>>> getDepartments(
            @RequestParam(required = false, defaultValue = "1") Long companyId,
            @RequestParam(required = false) Long branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.getDepartmentsByCompany(companyId, branchId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID", description = "Retrieve detailed information for a specific department")
    public ResponseEntity<ApiResponse<DepartmentDto>> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.getDepartmentById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Create department", description = "Add a new department for the company")
    public ResponseEntity<ApiResponse<DepartmentDto>> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request
    ) {
        DepartmentDto created = organizationService.createDepartment(request);
        return ResponseEntity.ok(ApiResponse.created("Department created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Update department", description = "Modify an existing department")
    public ResponseEntity<ApiResponse<DepartmentDto>> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request
    ) {
        DepartmentDto updated = organizationService.updateDepartment(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Department updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Delete department", description = "Delete a department if no employees or sub-departments are assigned")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable Long id) {
        organizationService.deleteDepartment(id);
        return ResponseEntity.ok(ApiResponse.ok("Department deleted successfully", null));
    }
}
