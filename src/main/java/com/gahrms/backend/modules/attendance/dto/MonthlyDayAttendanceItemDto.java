package com.gahrms.backend.modules.attendance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyDayAttendanceItemDto {
    private LocalDate date;
    private int dayOfMonth;
    private String dayOfWeek;
    private boolean isCurrentMonth;
    private boolean isToday;
    private boolean isFuture;

    @JsonProperty("isWeekOff")
    private boolean isWeekOff;

    private String firstHalfStatus;
    private String secondHalfStatus;
    private String displayCode;
    private String scheduledShift;

    @JsonProperty("isHoliday")
    private boolean isHoliday;

    private String holidayName;

    @JsonProperty("isLate")
    private boolean isLate;

    private int lateMinutes;
    private Integer lateOccurrenceNumber;

    @JsonProperty("isLatePenaltyApplied")
    private boolean isLatePenaltyApplied;

    @JsonProperty("isLwpPenaltyApplied")
    private boolean isLwpPenaltyApplied;

    private Instant firstIn;
    private Instant lastOut;
    private String workingHoursFormatted;
    private int punchCount;

    @Builder.Default
    private List<AttendancePunchResponse> punches = new ArrayList<>();
}