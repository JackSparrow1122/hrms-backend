package com.gahrms.backend.modules.leave.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestResponseDto {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String departmentName;
    private Long leaveTypeId;
    private String leaveTypeCode;
    private String leaveTypeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalDays;
    private boolean halfDay;
    private String halfDaySession;
    private String reason;
    private String status;
    private Long approverId;
    private String approverName;
    private Long companyId;
    private String companyName;
    private String reviewerRemarks;
    private Instant decidedAt;
    private Instant createdAt;
}
