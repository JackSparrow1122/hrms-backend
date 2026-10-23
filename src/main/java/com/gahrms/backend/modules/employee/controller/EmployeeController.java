package com.gahrms.backend.modules.employee.controller;

import com.gahrms.backend.common.dto.ApiResponse;
import com.gahrms.backend.common.dto.PageResponse;
import com.gahrms.backend.modules.employee.dto.*;
import com.gahrms.backend.modules.employee.entity.EmploymentStatus;
import com.gahrms.backend.modules.employee.service.EmployeeService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employee Lifecycle", description = "Endpoints for employee onboarding, directory search, profile updates, and status transitions")
@SecurityRequirement(name = "BearerAuth")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(
            summary = "Onboard new employee",
            description = "Creates a master employee record, assigns department/designation/branch, generates unique employee code, and optionally provisions login user account"
    )
    public ResponseEntity<ApiResponse<EmployeeResponse>> onboardedEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeResponse response = employeeService.onboardedEmployee(request);
        return ResponseEntity.ok(ApiResponse.created("Employee onboarded successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN', 'MANAGER', 'TEAM_LEAD', 'EMPLOYEE')")
    @Operation(
            summary = "Search and filter employee directory",
            description = "Paginated list of employees with optional search keyword (name, code, email) and filters (company, department, branch, designation, status)"
    )
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> getEmployees(
            @Parameter(description = "Filter by company ID") @RequestParam(required = false) Long companyId,
            @Parameter(description = "Search by name, code or email") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by department ID") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "Filter by branch ID") @RequestParam(required = false) Long branchId,
            @Parameter(description = "Filter by designation ID") @RequestParam(required = false) Long designationId,
            @Parameter(description = "Filter by employment status") @RequestParam(required = false) EmploymentStatus status,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<EmployeeResponse> result = employeeService.getEmployees(
                companyId, search, departmentId, branchId, designationId, status, pageable
        );
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee profile by ID", description = "Retrieve full profile information for a specific employee")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.getEmployeeById(id)));
    }

    @GetMapping("/code/{employeeCode}")
    @Operation(summary = "Get employee profile by employee code", description = "Retrieve profile information using the unique employee code")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeByCode(@PathVariable String employeeCode) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.getEmployeeByCode(employeeCode)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Update employee profile", description = "Modify employee personal, organizational or manager assignment details")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        EmployeeResponse updated = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Employee profile updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(
            summary = "Update employment lifecycle status",
            description = "Transitions employee status (ACTIVE, PROBATION, NOTICE_PERIOD, SUSPENDED, SEPARATED). Suspended/Separated states automatically deactivate linked login accounts."
    )
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeStatusRequest request
    ) {
        EmployeeResponse updated = employeeService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Employee status updated successfully", updated));
    }

    @GetMapping("/managers")
    @Operation(summary = "List eligible reporting managers", description = "Returns active employees in a company eligible to be assigned as managers")
    public ResponseEntity<ApiResponse<List<EmployeeSummaryDto>>> getEligibleManagers(
            @RequestParam Long companyId,
            @RequestParam(required = false) Long excludeEmployeeId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.getEligibleManagers(companyId, excludeEmployeeId)));
    }

    @GetMapping("/{id}/reports")
    @Operation(summary = "List direct reports", description = "Retrieve employees who report directly to this manager")
    public ResponseEntity<ApiResponse<List<EmployeeSummaryDto>>> getDirectReports(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.getDirectReports(id)));
    }

    @DeleteMapping("/{identifier}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN')")
    @Operation(summary = "Delete employee record", description = "Permanently removes employee, unlinks direct reports, and deprovisions associated user login account")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable String identifier) {
        employeeService.deleteEmployeeByIdentifier(identifier);
        return ResponseEntity.ok(ApiResponse.ok("Employee and associated account deleted successfully", null));
    }
}
