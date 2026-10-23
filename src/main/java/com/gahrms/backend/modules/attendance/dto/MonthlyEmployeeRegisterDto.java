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
public class MonthlyEmployeeRegisterDto {
    private int year;
    private int month;
    private String monthName;
    private int totalDaysInMonth;
    private int totalEmployees;
    @Builder.Default
    private List<MonthlyEmployeeRegisterItemDto> records = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyEmployeeRegisterItemDto {
        private Long employeeId;
        private String employeeCode;
        private String employeeName;
        private String departmentName;
        private String branchName;
        private String avatar;
        private int totalDays;
        private int workingDays;
        private int presentDays;
        private double leaveDays;
        private String leaveBreakdown; // e.g. "1.0d CL, 1.0d EL"
        private int halfDays;
        private int lateDays;
        private int latePenalties; // after 2 free late marks, count / 2
        private int absentDays;
        private int weekOffDays;
        private double payableDays;
        private int attendancePercentage;
        private int lateHalfDayPenalties;   // 4th, 6th, ... late => half day count
        private int lateLwpDays;            // 3rd, 5th, ... late => full LWP days
    }
}
