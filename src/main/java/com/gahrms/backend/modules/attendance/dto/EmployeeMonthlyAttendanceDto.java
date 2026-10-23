package com.gahrms.backend.modules.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeMonthlyAttendanceDto {
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String departmentName;
    private String branchName;
    private int year;
    private int lateLwpDays; 
    private int month;
    private String monthName; // e.g. "October 2026"
    private int totalDays;
    private int presentDays;
    private int lateDays;
    private int halfDays;
    private int absentDays;
    private int weekOffDays;
    @Builder.Default
    private List<MonthlyDayAttendanceItemDto> days = new ArrayList<>();
}
