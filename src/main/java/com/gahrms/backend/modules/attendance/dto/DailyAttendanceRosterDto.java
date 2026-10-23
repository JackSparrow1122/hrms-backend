package com.gahrms.backend.modules.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyAttendanceRosterDto {
    private LocalDate date;
    private int totalEmployees;
    private int presentCount;
    private int lateCount;
    private int halfDayCount;
    private int absentCount;
    private int onLeaveCount;
    private int weekOffCount;
    private boolean isWeekOffDay;
    @com.fasterxml.jackson.annotation.JsonProperty("isHoliday")
    private boolean isHoliday;
    private String holidayName;
    private String dayOfWeek;
    private String shiftExpectedIn;
    private String shiftExpectedOut;
    private String shiftPolicySummary;
    private int totalPunches;
    private int unmappedDevicePunchesCount;
    @Builder.Default
    private List<DailyEmployeeAttendanceItemDto> records = new ArrayList<>();
}
