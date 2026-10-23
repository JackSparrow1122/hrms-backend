package com.gahrms.backend.modules.attendance.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ETimeTrackRawPunchDto {

    private Long id;
    private String empCode;
    private Instant punchDate;
    private String updateFlag; // "IN" or "OUT"
}
