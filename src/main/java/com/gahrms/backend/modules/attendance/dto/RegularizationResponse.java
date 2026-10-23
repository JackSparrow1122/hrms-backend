package com.gahrms.backend.modules.attendance.dto;

import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegularizationResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private String departmentName;
    private LocalDate date;
    private Instant proposedPunchIn;
    private Instant proposedPunchOut;
    private String reason;
    private String status;
    private String reviewerRemarks;
    private Instant decidedAt;
    private Instant createdAt;
}