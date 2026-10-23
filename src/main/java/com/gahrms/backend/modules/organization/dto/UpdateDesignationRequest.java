package com.gahrms.backend.modules.organization.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDesignationRequest {

    @NotBlank(message = "Designation name is required")
    private String name;

    @NotBlank(message = "Designation code is required")
    private String code;

    private Long departmentId;

    private Long branchId;

    @Min(value = 1, message = "Level must be at least 1")
    private Integer level;

    private Boolean active;
}
