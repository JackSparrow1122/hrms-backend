package com.gahrms.backend.modules.organization.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDepartmentRequest {

    @Size(min = 2, max = 150, message = "Department name must be between 2 and 150 characters")
    private String name;

    @Size(min = 2, max = 50, message = "Department code must be between 2 and 50 characters")
    private String code;

    private Long branchId;

    private Long parentDepartmentId;

    private Boolean active;
}
