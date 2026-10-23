package com.gahrms.backend.modules.attendance.controller;

import com.gahrms.backend.common.dto.ApiResponse;
import com.gahrms.backend.modules.attendance.dto.RegularizationResponse;
import com.gahrms.backend.modules.attendance.entity.RegularizationRequest;
import com.gahrms.backend.modules.attendance.repository.RegularizationRepository;
import com.gahrms.backend.modules.employee.entity.Employee;
import com.gahrms.backend.modules.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/regularization")
@RequiredArgsConstructor
public class RegularizationController {

    private final RegularizationRepository regularizationRepository;
    private final EmployeeRepository employeeRepository;

    // Helper: Entity -> DTO
    private RegularizationResponse toDto(RegularizationRequest r) {
        Employee emp = r.getEmployee();
        return RegularizationResponse.builder()
                .id(r.getId())
                .employeeId(emp != null ? emp.getId() : null)
                .employeeName(emp != null ? emp.getFullName() : "Unknown Employee")
                .employeeCode(emp != null ? emp.getEmployeeCode() : "--")
                .departmentName(emp != null && emp.getDepartment() != null ? emp.getDepartment().getName() : "General")
                .date(r.getDate())
                .proposedPunchIn(r.getProposedPunchIn())
                .proposedPunchOut(r.getProposedPunchOut())
                .reason(r.getReason())
                .status(r.getStatus())
                .reviewerRemarks(r.getReviewerRemarks())
                .decidedAt(r.getDecidedAt())
                .createdAt(r.getCreatedAt())
                .build();
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<RegularizationResponse>> applyCorrection(@RequestBody Map<String, Object> payload) {
        Long employeeId = Long.valueOf(payload.get("employeeId").toString());
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        java.time.LocalDate requestDate = java.time.LocalDate.parse(payload.get("date").toString());

        long activeCount = regularizationRepository.countActiveRequestsForDate(employeeId, requestDate);
        if (activeCount > 0) {
            return ResponseEntity.status(400).body(
                    ApiResponse.ok("Aapne is date (" + requestDate + ") ke liye already correction request submit kar di hai. Purani request reject hone ke baad hi naya submit kar sakte hain.", null)
            );
        }

        RegularizationRequest request = RegularizationRequest.builder()
                .employee(employee)
                .date(requestDate)
                .proposedPunchIn(Instant.parse(payload.get("proposedPunchIn").toString()))
                .proposedPunchOut(Instant.parse(payload.get("proposedPunchOut").toString()))
                .reason(payload.get("reason").toString())
                .status("PENDING")
                .build();

        RegularizationRequest saved = regularizationRepository.save(request);
        return ResponseEntity.ok(ApiResponse.created("Correction request submitted successfully", toDto(saved)));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<RegularizationResponse>>> getEmployeeRequests(@PathVariable Long employeeId) {
        List<RegularizationResponse> list = regularizationRepository
                .findByEmployee_IdOrderByCreatedAtDesc(employeeId)
                .stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Employee regularization requests", list));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<RegularizationResponse>>> getPendingRequests() {
        List<RegularizationResponse> list = regularizationRepository
                .findByStatusOrderByCreatedAtDesc("PENDING")
                .stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Pending regularization requests", list));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN', 'MANAGER', 'TEAM_LEAD')")
    public ResponseEntity<?> approveRequest(@PathVariable Long id) {
        try {
            RegularizationRequest request = regularizationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Request not found"));

            if (!"PENDING".equals(request.getStatus())) {
                return ResponseEntity.badRequest().body("Only PENDING requests can be approved");
            }

            request.setStatus("APPROVED");
            request.setDecidedAt(Instant.now());
            request.setReviewerRemarks("Approved by HR Admin");
            regularizationRepository.save(request);

            return ResponseEntity.ok("Request approved successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'HR_ADMIN', 'MANAGER', 'TEAM_LEAD')")
    public ResponseEntity<?> rejectRequest(@PathVariable Long id) {
        try {
            RegularizationRequest request = regularizationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Request not found"));

            if (!"PENDING".equals(request.getStatus())) {
                return ResponseEntity.badRequest().body("Only PENDING requests can be rejected");
            }

            request.setStatus("REJECTED");
            request.setDecidedAt(Instant.now());
            request.setReviewerRemarks("Rejected by HR Admin");
            regularizationRepository.save(request);

            return ResponseEntity.ok("Request rejected successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}