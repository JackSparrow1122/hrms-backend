package com.gahrms.backend.modules.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiometricEmployeeMappingDto {

    private Long id;
    private String sourceSystem;

    @NotBlank(message = "Biometric employee code is required (e.g., 'GA01')")
    private String biometricEmpCode;

    @NotNull(message = "HRMS employee ID is required")
    private Long employeeId;

    private String employeeCode;
    private String employeeName;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
