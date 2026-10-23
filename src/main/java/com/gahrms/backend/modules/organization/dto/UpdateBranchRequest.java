package com.gahrms.backend.modules.organization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBranchRequest {

    @NotBlank(message = "Branch name is required")
    private String name;

    private String city;
    private String state;
    private String country;
    private Boolean headquarters;
}
