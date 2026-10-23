package com.gahrms.backend.modules.organization.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gahrms.backend.modules.organization.entity.TenantStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateTenantRequest {

    @NotBlank(message = "Company name is required")
    private String name;

    private String registrationNumber;

    private String taxId;

    private String timezone;

    private String currency;

    private String subscriptionPlan;

    private Integer maxEmployees;
    private Integer maxBranches;

    @Email(message = "Invalid contact email")
    private String contactEmail;

    private String contactPhone;

    private String domain;

    private TenantStatus status;
}
