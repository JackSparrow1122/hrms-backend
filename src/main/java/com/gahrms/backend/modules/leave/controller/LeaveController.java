package com.gahrms.backend.modules.leave.controller;

import com.gahrms.backend.common.dto.ApiResponse;
import com.gahrms.backend.modules.leave.dto.*;
import com.gahrms.backend.modules.leave.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
@Tag(name = "Leave Management", description = "Endpoints for employee leave applications, quotas, and approvals")
@SecurityRequirement(name = "BearerAuth")
public class LeaveController {

    private final LeaveService leaveService;

    @GetMapping("/balances")
    @Operation(summary = "Get employee leave balances", description = "Retrieves EL, SL, CL, and LWP balances for employee in year 2026")
    public ResponseEntity<ApiResponse<List<LeaveBalanceDto>>> getLeaveBalances(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false, defaultValue = "2026") Integer year
    ) {
        List<LeaveBalanceDto> balances = leaveService.getLeaveBalances(employeeId, year);
        return ResponseEntity.ok(ApiResponse.ok("Leave balances retrieved successfully", balances));
    }

    @GetMapping("/my-requests")
    @Operation(summary = "Get employee leave requests", description = "Retrieves leave request history for the employee")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponseDto>>> getMyRequests(
            @RequestParam(required = false) Long employeeId
    ) {
        List<LeaveRequestResponseDto> requests = leaveService.getMyLeaveRequests(employeeId);
        return ResponseEntity.ok(ApiResponse.ok("Leave requests retrieved successfully", requests));
    }

    @PostMapping("/apply")
    @Operation(summary = "Apply for leave", description = "Submits a full-day or half-day leave request with designated approver")
    public ResponseEntity<ApiResponse<LeaveRequestResponseDto>> applyLeave(
            @Valid @RequestBody ApplyLeaveRequest request
    ) {
        LeaveRequestResponseDto response = leaveService.applyLeave(request);
        return ResponseEntity.ok(ApiResponse.ok("Leave application submitted successfully", response));
    }

    @GetMapping("/pending-approvals")
    @Operation(summary = "Get pending leave approvals", description = "Retrieves pending leave requests for TL, Manager, or HR Admin, optionally filtered by company")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponseDto>>> getPendingApprovals(
            @RequestParam(required = false) Long companyId
    ) {
        List<LeaveRequestResponseDto> pending = leaveService.getPendingApprovals(companyId);
        return ResponseEntity.ok(ApiResponse.ok("Pending leave approvals retrieved successfully", pending));
    }

    @GetMapping("/approvals")
    @Operation(summary = "Get leave approvals", description = "Retrieves leave requests for TL, Manager, or HR Admin filtered by status (ALL, PENDING, APPROVED, REJECTED) and optional companyId")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponseDto>>> getApprovals(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false, defaultValue = "ALL") String status
    ) {
        List<LeaveRequestResponseDto> list = leaveService.getApprovals(companyId, status);
        return ResponseEntity.ok(ApiResponse.ok("Leave approvals retrieved successfully", list));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve leave request", description = "Approves a pending leave request and updates employee quotas")
    public ResponseEntity<ApiResponse<LeaveRequestResponseDto>> approveLeave(
            @PathVariable Long id,
            @RequestBody(required = false) LeaveApprovalRequest request
    ) {
        LeaveRequestResponseDto approved = leaveService.approveLeave(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Leave approved successfully", approved));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject leave request", description = "Rejects a pending leave request and restores employee quotas")
    public ResponseEntity<ApiResponse<LeaveRequestResponseDto>> rejectLeave(
            @PathVariable Long id,
            @RequestBody(required = false) LeaveApprovalRequest request
    ) {
        LeaveRequestResponseDto rejected = leaveService.rejectLeave(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Leave rejected", rejected));
    }
}
