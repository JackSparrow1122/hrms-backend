package com.gahrms.backend.modules.attendance.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncSummaryDto {

    private String status; // "SUCCESS", "NO_NEW_RECORDS", "SOURCE_OFFLINE", "LOCKED", "ERROR"
    private int fetchedCount;
    private int insertedCount;
    private int skippedDuplicatesCount;
    private int unmappedEmployeesCount;
    private Long previousCursorId;
    private Long newCursorId;
    private String message;
    private long durationMs;
    private Instant completedAt;
}
