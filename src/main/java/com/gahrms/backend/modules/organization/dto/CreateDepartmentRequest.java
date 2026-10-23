package com.gahrms.backend.modules.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDepartmentRequest {

    @NotNull(message = "Company ID is required")
    private Long companyId;

    private Long branchId;

    private Long parentDepartmentId;

    @NotBlank(message = "Department name is required")
    private String name;

    @NotBlank(message = "Department code is required")
    private String code;
}
