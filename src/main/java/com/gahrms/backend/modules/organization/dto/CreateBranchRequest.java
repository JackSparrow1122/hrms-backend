package com.gahrms.backend.modules.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBranchRequest {

    @NotNull(message = "Company ID is required")
    private Long companyId;

    @NotBlank(message = "Branch name is required")
    private String name;

    @NotBlank(message = "Branch code is required")
    private String code;

    private String city;
    private String state;
    private String country;
    private boolean headquarters;

    // Optional HR Admin details for instant onboarding
    private String hrAdminFirstName;
    private String hrAdminLastName;
    private String hrAdminEmail;
    private String hrAdminPassword;
    private String hrAdminPhone;
}
