package com.gahrms.backend.modules.organization.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDesignationRequest {

    @NotNull(message = "Company ID is required")
    private Long companyId;

    private Long branchId;

    private Long departmentId;

    @NotBlank(message = "Designation name is required")
    private String name;

    @NotBlank(message = "Designation code is required")
    private String code;

    @Min(value = 1, message = "Level must be at least 1")
    @Builder.Default
    private Integer level = 1;
}
