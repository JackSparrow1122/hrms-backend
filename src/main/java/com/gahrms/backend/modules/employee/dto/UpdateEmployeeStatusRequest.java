package com.gahrms.backend.modules.employee.dto;

import com.gahrms.backend.modules.employee.entity.EmploymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEmployeeStatusRequest {

    @NotNull(message = "Status is required")
    private EmploymentStatus status;

    private String remarks;
}
