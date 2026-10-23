package com.gahrms.backend.modules.attendance.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendancePunchResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private String sourceSystem;
    private Long sourceRecordId;
    private Instant punchTime;
    private String punchType;
    private String mappingStatus;
    private String rawPayload;
    private Instant createdAt;
}
