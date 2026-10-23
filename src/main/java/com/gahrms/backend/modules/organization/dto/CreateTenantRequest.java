package com.gahrms.backend.modules.organization.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateTenantRequest {

    // Organization Information
    @NotBlank(message = "Company legal name is required")
    private String name;

    @NotBlank(message = "Unique company code is required (e.g. TCS, INFY, GRYPHON)")
    private String code;

    private String registrationNumber;

    private String taxId;

    @Builder.Default
    private String timezone = "Asia/Kolkata";

    @Builder.Default
    private String currency = "INR";

    @Builder.Default
    private String subscriptionPlan = "ENTERPRISE";

    @Builder.Default
    private Integer maxEmployees = 500;

    @Builder.Default
    private Integer maxBranches = 5;

    @Email(message = "Invalid contact email")
    private String contactEmail;

    private String contactPhone;

    private String domain;

    // Initial Tenant Admin Setup
    @NotBlank(message = "Tenant Admin work email is required")
    @Email(message = "Invalid admin email")
    private String adminEmail;

    private String adminPassword;

    @NotBlank(message = "Tenant Admin first name is required")
    private String adminFirstName;

    @NotBlank(message = "Tenant Admin last name is required")
    private String adminLastName;

    private String adminPhoneNumber;
}
