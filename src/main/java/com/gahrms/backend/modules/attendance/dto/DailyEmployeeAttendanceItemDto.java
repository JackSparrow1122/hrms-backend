package com.gahrms.backend.modules.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyEmployeeAttendanceItemDto {
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String departmentName;
    private String branchName;
    private String avatar;
    private Instant firstIn;
    private Instant lastOut;
    private Integer workedMinutes;
    private String workingHoursFormatted;
    private Integer lateMinutes;
    private String status; // PRESENT, LATE, HALF_DAY, ABSENT, UNMAPPED_DEVICE
    private int punchCount;
    private boolean biometricMapped;
    private String biometricCode;
    private String expectedIn;
    private String expectedOut;
    private boolean isWeekOff;
    @com.fasterxml.jackson.annotation.JsonProperty("isHoliday")
    private boolean isHoliday;
    private String holidayName;
    private Integer earlyDepartureMinutes;
    @Builder.Default
    private List<AttendancePunchResponse> punches = new ArrayList<>();
}
