package com.gahrms.backend.modules.leave.service;

import com.gahrms.backend.common.exception.BadRequestException;
import com.gahrms.backend.common.exception.ResourceNotFoundException;
import com.gahrms.backend.common.exception.UnauthorizedException;
import com.gahrms.backend.common.util.SecurityUtils;
import com.gahrms.backend.modules.employee.entity.Employee;
import com.gahrms.backend.modules.employee.repository.EmployeeRepository;
import com.gahrms.backend.modules.identity.entity.User;
import com.gahrms.backend.modules.identity.repository.UserRepository;
import com.gahrms.backend.modules.leave.dto.*;
import com.gahrms.backend.modules.leave.entity.LeaveBalance;
import com.gahrms.backend.modules.leave.entity.LeaveRequest;
import com.gahrms.backend.modules.leave.entity.LeaveType;
import com.gahrms.backend.modules.leave.repository.LeaveBalanceRepository;
import com.gahrms.backend.modules.leave.repository.LeaveRequestRepository;
import com.gahrms.backend.modules.leave.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    /**
     * Resolves currently authenticated employee.
     */
    public Employee getCurrentEmployee() {
        String email = SecurityUtils.getCurrentUserEmail()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (user.getEmployeeId() == null) {
            // Check if Super Admin or platform user with no employee record
            return null;
        }

        return employeeRepository.findById(user.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", user.getEmployeeId()));
    }

    private boolean isHrOrAdmin() {
        return SecurityUtils.isCurrentUserSuperAdmin()
                || SecurityUtils.hasAuthority("ROLE_HR_ADMIN")
                || SecurityUtils.hasAuthority("ROLE_TENANT_ADMIN");
    }

    /**
     * Fetch leave balances for employee for a year (default 2026).
     */
    @Transactional(readOnly = true)
    public List<LeaveBalanceDto> getLeaveBalances(Long targetEmployeeId, Integer year) {
        int yr = year != null ? year : 2026;
        Employee currentEmp = getCurrentEmployee();

        Long empId = targetEmployeeId;
        if (!isHrOrAdmin() || empId == null) {
            if (currentEmp == null) {
                return Collections.emptyList();
            }
            empId = currentEmp.getId();
        }

        List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeIdAndYear(empId, yr);
        List<LeaveBalanceDto> result = balances.stream().map(b -> LeaveBalanceDto.builder()
                .id(b.getId())
                .leaveTypeId(b.getLeaveType().getId())
                .leaveTypeCode(b.getLeaveType().getCode())
                .leaveTypeName(b.getLeaveType().getName())
                .year(b.getYear())
                .totalAllowance(b.getTotalAllowance())
                .usedCount(b.getUsedCount())
                .pendingCount(b.getPendingCount())
                .remainingCount(b.getRemainingCount())
                .isPaid(b.getLeaveType().isPaid())
                .build()).collect(Collectors.toList());

        // Always ensure LWP (Leave Without Pay) is present in the list
        boolean hasLwp = result.stream().anyMatch(b -> "LWP".equalsIgnoreCase(b.getLeaveTypeCode()));
        if (!hasLwp) {
            result.add(LeaveBalanceDto.builder()
                    .id(-1L)
                    .leaveTypeId(-1L)
                    .leaveTypeCode("LWP")
                    .leaveTypeName("Leave Without Pay")
                    .year(yr)
                    .totalAllowance(BigDecimal.ZERO)
                    .usedCount(BigDecimal.ZERO)
                    .pendingCount(BigDecimal.ZERO)
                    .remainingCount(BigDecimal.valueOf(999))
                    .isPaid(false)
                    .build());
        }

        return result;
    }

    /**
     * Fetch leave requests for current employee (or specified employee if HR Admin).
     */
    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDto> getMyLeaveRequests(Long targetEmployeeId) {
        Employee currentEmp = getCurrentEmployee();
        Long empId = targetEmployeeId;
        if (!isHrOrAdmin() || empId == null) {
            if (currentEmp == null) return Collections.emptyList();
            empId = currentEmp.getId();
        }

        List<LeaveRequest> requests = leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(empId);
        return requests.stream().map(this::mapToDto).collect(Collectors.toList());
    }

@Transactional
public LeaveRequestResponseDto applyLeave(ApplyLeaveRequest request) {
    // SELF-SERVICE ONLY: always use the logged-in employee
    Employee employee = getCurrentEmployee();
    if (employee == null) {
        throw new BadRequestException(
            "Aapka employee record nahi mila. Leave apply karne ke liye employee profile zaroori hai. HR se contact karein."
        );
    }
    Long empId = employee.getId();

    LocalDate effectiveEndDate = request.isHalfDay() ? request.getStartDate() : request.getEndDate();
    if (request.getStartDate().isAfter(effectiveEndDate)) {
        throw new BadRequestException("Start date cannot be after end date");
    }

    boolean alreadyApplied = leaveRequestRepository.countOverlappingLeave(empId, request.getStartDate(), effectiveEndDate) > 0;
    if (alreadyApplied) {
        throw new BadRequestException("Already applied: A leave request already exists for this date. You cannot apply multiple leaves on the same day.");
    }

    BigDecimal days;
    if (request.isHalfDay()) {
        days = BigDecimal.valueOf(0.5);
    } else {
        long diff = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        days = BigDecimal.valueOf(diff);
    }

    String code = request.getLeaveTypeCode().toUpperCase().trim();
    LeaveType leaveType = leaveTypeRepository.findByCompanyIdAndCodeIgnoreCase(employee.getCompany().getId(), code)
            .or(() -> leaveTypeRepository.findByCodeIgnoreCase(code))
            .orElseThrow(() -> new BadRequestException("Invalid leave type code: " + code));

    if (leaveType.isPaid()) {
        int year = request.getStartDate().getYear();
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(empId, leaveType.getId(), year)
                .orElseThrow(() -> new BadRequestException("No leave balance configured for " + code + " in year " + year));

        if (balance.getRemainingCount().compareTo(days) < 0) {
            throw new BadRequestException("Insufficient " + code + " balance. Remaining: " 
                    + balance.getRemainingCount() + ", Requested: " + days + ". Consider applying as LWP (Leave Without Pay).");
        }

        balance.setRemainingCount(balance.getRemainingCount().subtract(days));
        balance.setPendingCount(balance.getPendingCount().add(days));
        balance.setUpdatedAt(Instant.now());
        leaveBalanceRepository.save(balance);
    }

    // Approver resolution — same as before (manager_id driven)
    Employee approver = employee.getManager();
    if (approver == null) {
        Long companyId = employee.getCompany() != null ? employee.getCompany().getId() : null;
        if (companyId != null) {
            List<Long> tenantAdminEmpIds = userRepository.findTenantAdminEmployeeIds(companyId);
            if (tenantAdminEmpIds != null && !tenantAdminEmpIds.isEmpty()) {
                approver = employeeRepository.findById(tenantAdminEmpIds.get(0)).orElse(null);
            }
        }
    }

    LeaveRequest lr = LeaveRequest.builder()
            .employee(employee)
            .leaveType(leaveType)
            .startDate(request.getStartDate())
            .endDate(request.isHalfDay() ? request.getStartDate() : request.getEndDate())
            .totalDays(days)
            .halfDay(request.isHalfDay())
            .halfDaySession(request.isHalfDay() ? request.getHalfDaySession() : null)
            .reason(request.getReason())
            .status("PENDING")
            .approver(approver)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    lr = leaveRequestRepository.save(lr);
    log.info("Leave request #{} submitted by {} ({}) for {} days {} to approver {}",
            lr.getId(), employee.getFullName(), employee.getEmployeeCode(), days, code,
            approver != null ? approver.getFullName() : "HR Admin");

    return mapToDto(lr);
}

    /**
     * Get pending approvals for TL, Manager, or HR Admin.
     */
    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDto> getPendingApprovals() {
        return getApprovals(null, "PENDING");
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDto> getPendingApprovals(Long companyId) {
        return getApprovals(companyId, "PENDING");
    }

    /**
     * Get leave approvals filtered by status (ALL, PENDING, APPROVED, REJECTED) and optional companyId.
     * HR Admin / Super Admin sees company-scoped or global requests; Team Leads / Managers see requests for their team.
     */
    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDto> getApprovals(String status) {
        return getApprovals(null, status);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDto> getApprovals(Long companyId, String status) {
        Employee currentEmp = getCurrentEmployee();

        List<LeaveRequest> requests;
        boolean filterStatus = status != null && !status.equalsIgnoreCase("ALL");
        String targetStatus = filterStatus ? status.toUpperCase().trim() : null;

        if (isHrOrAdmin()) {
            if (companyId != null) {
                if (filterStatus) {
                    requests = leaveRequestRepository.findByCompanyIdAndStatus(companyId, targetStatus);
                } else {
                    requests = leaveRequestRepository.findByCompanyId(companyId);
                }
            } else {
                if (filterStatus) {
                    requests = leaveRequestRepository.findByStatusOrderByCreatedAtDesc(targetStatus);
                } else {
                    requests = leaveRequestRepository.findAllByOrderByCreatedAtDesc();
                }
            }
        } else if (currentEmp != null) {
            // TL or Manager sees requests where they are assigned approver or direct manager
            if (filterStatus) {
                requests = leaveRequestRepository.findByApproverOrManagerAndStatus(currentEmp.getId(), targetStatus);
            } else {
                requests = leaveRequestRepository.findByApproverOrManager(currentEmp.getId());
            }
        } else {
            requests = Collections.emptyList();
        }

        return requests.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Approve leave request.
     */
    @Transactional
    public LeaveRequestResponseDto approveLeave(Long requestId, LeaveApprovalRequest approvalRequest) {
        LeaveRequest lr = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", requestId));

        if (!"PENDING".equalsIgnoreCase(lr.getStatus())) {
            throw new BadRequestException("Leave request is already " + lr.getStatus());
        }

        Employee currentEmp = getCurrentEmployee();
        // Self-approval is allowed ONLY if the leave request's designated approver is the employee themselves
        // (i.e., their reporting manager is set to self — e.g., HR Admin whose manager is HR Admin herself)
        boolean isSelfApprovalAllowed = lr.getApprover() != null
                && lr.getEmployee().getId().equals(lr.getApprover().getId());
        if (currentEmp != null && lr.getEmployee().getId().equals(currentEmp.getId())
                && !isSelfApprovalAllowed && !isHrOrAdmin()) {
            throw new UnauthorizedException("You cannot approve your own leave request. It must be approved by your reporting manager.");
        }

        boolean authorized = isHrOrAdmin()
                || (currentEmp != null && lr.getApprover() != null && lr.getApprover().getId().equals(currentEmp.getId()))
                || (currentEmp != null && lr.getEmployee().getManager() != null && lr.getEmployee().getManager().getId().equals(currentEmp.getId()));

        if (!authorized) {
            throw new UnauthorizedException("You are not authorized to approve this leave request");
        }

        lr.setStatus("APPROVED");
        lr.setReviewerRemarks(approvalRequest != null ? approvalRequest.getRemarks() : "Approved");
        lr.setDecidedAt(Instant.now());
        lr.setUpdatedAt(Instant.now());

        // Update leave balance: pending decreases, used increases
        BigDecimal approvedDays = lr.getTotalDays();
        if (lr.getLeaveType().isPaid()) {
            int year = lr.getStartDate().getYear();
            leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(lr.getEmployee().getId(), lr.getLeaveType().getId(), year)
                    .ifPresent(balance -> {
                        balance.setPendingCount(balance.getPendingCount().subtract(approvedDays));
                        balance.setUsedCount(balance.getUsedCount().add(approvedDays));
                        balance.setUpdatedAt(Instant.now());
                        leaveBalanceRepository.save(balance);
                    });
        }

        lr = leaveRequestRepository.save(lr);
        log.info("Leave request #{} APPROVED by user for {}", lr.getId(), lr.getEmployee().getFullName());
        return mapToDto(lr);
    }

    /**
     * Reject leave request.
     */
    @Transactional
    public LeaveRequestResponseDto rejectLeave(Long requestId, LeaveApprovalRequest approvalRequest) {
        LeaveRequest lr = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", requestId));

        if (!"PENDING".equalsIgnoreCase(lr.getStatus())) {
            throw new BadRequestException("Leave request is already " + lr.getStatus());
        }

        Employee currentEmp = getCurrentEmployee();
        boolean isSelfApprovalAllowed = lr.getApprover() != null
                && lr.getEmployee().getId().equals(lr.getApprover().getId());
        if (currentEmp != null && lr.getEmployee().getId().equals(currentEmp.getId())
                && !isSelfApprovalAllowed && !isHrOrAdmin()) {
            throw new UnauthorizedException("You cannot reject your own leave request.");
        }

        boolean authorized = isHrOrAdmin()
                || (currentEmp != null && lr.getApprover() != null && lr.getApprover().getId().equals(currentEmp.getId()))
                || (currentEmp != null && lr.getEmployee().getManager() != null && lr.getEmployee().getManager().getId().equals(currentEmp.getId()));

        if (!authorized) {
            throw new UnauthorizedException("You are not authorized to reject this leave request");
        }

        lr.setStatus("REJECTED");
        lr.setReviewerRemarks(approvalRequest != null ? approvalRequest.getRemarks() : "Rejected");
        lr.setDecidedAt(Instant.now());
        lr.setUpdatedAt(Instant.now());

        // Restore leave balance: pending decreases, remaining restored
        BigDecimal rejectedDays = lr.getTotalDays();
        if (lr.getLeaveType().isPaid()) {
            int year = lr.getStartDate().getYear();
            leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(lr.getEmployee().getId(), lr.getLeaveType().getId(), year)
                    .ifPresent(balance -> {
                        balance.setPendingCount(balance.getPendingCount().subtract(rejectedDays));
                        balance.setRemainingCount(balance.getRemainingCount().add(rejectedDays));
                        balance.setUpdatedAt(Instant.now());
                        leaveBalanceRepository.save(balance);
                    });
        }

        lr = leaveRequestRepository.save(lr);
        log.info("Leave request #{} REJECTED by user for {}", lr.getId(), lr.getEmployee().getFullName());
        return mapToDto(lr);
    }

    private LeaveRequestResponseDto mapToDto(LeaveRequest lr) {
        return LeaveRequestResponseDto.builder()
                .id(lr.getId())
                .employeeId(lr.getEmployee().getId())
                .employeeCode(lr.getEmployee().getEmployeeCode())
                .employeeName(lr.getEmployee().getFullName())
                .departmentName(lr.getEmployee().getDepartment() != null ? lr.getEmployee().getDepartment().getName() : "General")
                .leaveTypeId(lr.getLeaveType().getId())
                .leaveTypeCode(lr.getLeaveType().getCode())
                .leaveTypeName(lr.getLeaveType().getName())
                .startDate(lr.getStartDate())
                .endDate(lr.getEndDate())
                .totalDays(lr.getTotalDays())
                .halfDay(lr.isHalfDay())
                .halfDaySession(lr.getHalfDaySession())
                .reason(lr.getReason())
                .status(lr.getStatus())
                .approverId(lr.getApprover() != null ? lr.getApprover().getId() : null)
                .approverName(lr.getApprover() != null ? lr.getApprover().getFullName() : "HR Admin")
                .reviewerRemarks(lr.getReviewerRemarks())
                .companyId(lr.getEmployee().getCompany() != null ? lr.getEmployee().getCompany().getId() : null)
                .companyName(lr.getEmployee().getCompany() != null ? lr.getEmployee().getCompany().getName() : null)
                .decidedAt(lr.getDecidedAt())
                .createdAt(lr.getCreatedAt())
                .build();
    }
}
