package com.gahrms.backend.modules.identity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gahrms.backend.modules.identity.entity.ScopeType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateUserRequest {

    @NotBlank(message = "Work email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String password;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String phoneNumber;

    private Long companyId;

    @Builder.Default
    private ScopeType scopeType = ScopeType.ORGANIZATION;

    private Long scopeBranchId;

    private Long scopeDepartmentId;

    private Long employeeId;

    @NotEmpty(message = "At least one role is required")
    private Set<String> roles;
}
