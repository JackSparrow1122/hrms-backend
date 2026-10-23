package com.gahrms.backend.modules.attendance.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionDiagnosticResult {

    private boolean connected;
    private String status; // "SUCCESS", "DISABLED", "CONNECTION_REFUSED", "AUTHENTICATION_FAILED", "TABLE_NOT_FOUND", "TIMEOUT", "ERROR"
    private String message;
    private String targetHost;
    private int targetPort;
    private String targetDatabase;
    private boolean sourceTableAccessible;
    private Long sampleRecordCount;
    private Instant checkedAt;
}
