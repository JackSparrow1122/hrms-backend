package com.gahrms.backend.modules.organization.controller;

import com.gahrms.backend.common.dto.ApiResponse;
import com.gahrms.backend.modules.organization.dto.*;
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
@RequestMapping("/api/v1/organization")
@RequiredArgsConstructor
@Tag(name = "Organization Structure", description = "Endpoints for companies, branches, departments, and designations")
@SecurityRequirement(name = "BearerAuth")
public class OrganizationController {

    private final OrganizationService organizationService;

    // --- Companies ---
    @GetMapping("/companies")
    @Operation(summary = "List all companies", description = "Retrieve list of all registered legal entities / companies")
    public ResponseEntity<ApiResponse<List<CompanyDto>>> getAllCompanies() {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.getAllCompanies()));
    }

    @GetMapping("/companies/{id}")
    @Operation(summary = "Get company by ID", description = "Retrieve detailed information for a specific company")
    public ResponseEntity<ApiResponse<CompanyDto>> getCompanyById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.getCompanyById(id)));
    }

    // --- Branches ---
    @GetMapping("/companies/{companyId}/branches")
    @Operation(summary = "List branches", description = "Retrieve all branch locations for a specific company")
    public ResponseEntity<ApiResponse<List<BranchDto>>> getBranches(@PathVariable Long companyId) {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.getBranchesByCompany(companyId)));
    }

    @GetMapping("/branches/{id}")
    @Operation(summary = "Get branch by ID", description = "Retrieve details for a specific branch")
    public ResponseEntity<ApiResponse<BranchDto>> getBranchById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.getBranchById(id)));
    }

    @PostMapping("/branches")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Create branch", description = "Add a new branch office to a company (Head HR / Tenant Admin only, up to allocated limit)")
    public ResponseEntity<ApiResponse<BranchDto>> createBranch(@Valid @RequestBody CreateBranchRequest request) {
        BranchDto created = organizationService.createBranch(request);
        return ResponseEntity.ok(ApiResponse.created("Branch created successfully", created));
    }

    @PutMapping("/branches/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Update branch", description = "Update branch details (Head HR / Tenant Admin only)")
    public ResponseEntity<ApiResponse<BranchDto>> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBranchRequest request
    ) {
        BranchDto updated = organizationService.updateBranch(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Branch updated successfully", updated));
    }

    @DeleteMapping("/branches/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Delete branch", description = "Delete a branch if no active workforce is assigned (Head HR / Tenant Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable Long id) {
        organizationService.deleteBranch(id);
        return ResponseEntity.ok(ApiResponse.ok("Branch deleted successfully", null));
    }

    @PostMapping("/branches/{id}/hr-admin")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Assign Branch HR Admin", description = "Assign or provision a dedicated Branch HR Admin for a branch (Head HR / Tenant Admin only)")
    public ResponseEntity<ApiResponse<BranchDto>> assignBranchHrAdmin(
            @PathVariable Long id,
            @Valid @RequestBody AssignBranchHrRequest request
    ) {
        BranchDto updated = organizationService.assignBranchHrAdmin(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Branch HR Admin assigned successfully", updated));
    }

    // --- Departments ---
    @GetMapping("/companies/{companyId}/departments")
    @Operation(summary = "List departments", description = "Retrieve active departments for a specific company, optionally filtered by branch")
    public ResponseEntity<ApiResponse<List<DepartmentDto>>> getDepartments(
            @PathVariable Long companyId,
            @RequestParam(required = false) Long branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.getDepartmentsByCompany(companyId, branchId)));
    }

    @GetMapping("/departments/{id}")
    @Operation(summary = "Get department by ID", description = "Retrieve details for a specific department")
    public ResponseEntity<ApiResponse<DepartmentDto>> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.getDepartmentById(id)));
    }

    @PostMapping("/departments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Create department", description = "Add a new department with optional parent department hierarchy")
    public ResponseEntity<ApiResponse<DepartmentDto>> createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        DepartmentDto created = organizationService.createDepartment(request);
        return ResponseEntity.ok(ApiResponse.created("Department created successfully", created));
    }

    @PutMapping("/departments/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Update department", description = "Modify an existing department")
    public ResponseEntity<ApiResponse<DepartmentDto>> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request
    ) {
        DepartmentDto updated = organizationService.updateDepartment(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Department updated successfully", updated));
    }

    @DeleteMapping("/departments/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Delete department", description = "Delete a department if no employees or sub-departments are assigned")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable Long id) {
        organizationService.deleteDepartment(id);
        return ResponseEntity.ok(ApiResponse.ok("Department deleted successfully", null));
    }

    // --- Designations ---
    @GetMapping("/companies/{companyId}/designations")
    @Operation(summary = "List designations", description = "Retrieve designations/job titles ordered by seniority level, optionally filtered by department and branch")
    public ResponseEntity<ApiResponse<List<DesignationDto>>> getDesignations(
            @PathVariable Long companyId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.getDesignationsByCompany(companyId, departmentId, branchId)));
    }

    @PostMapping("/designations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Create designation", description = "Add a new designation/job title with seniority level")
    public ResponseEntity<ApiResponse<DesignationDto>> createDesignation(@Valid @RequestBody CreateDesignationRequest request) {
        DesignationDto created = organizationService.createDesignation(request);
        return ResponseEntity.ok(ApiResponse.created("Designation created successfully", created));
    }
}
