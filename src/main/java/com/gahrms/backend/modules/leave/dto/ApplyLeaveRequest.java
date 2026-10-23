package com.gahrms.backend.modules.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyLeaveRequest {

    @NotBlank(message = "Leave type code is required (e.g. EL, SL, CL, LWP)")
    private String leaveTypeCode;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private boolean halfDay;

    private String halfDaySession; // FIRST_HALF or SECOND_HALF

    @NotBlank(message = "Reason is required")
    private String reason;
}
