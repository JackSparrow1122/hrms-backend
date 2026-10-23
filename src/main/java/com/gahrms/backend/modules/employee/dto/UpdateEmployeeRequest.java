package com.gahrms.backend.modules.employee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEmployeeRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String phoneNumber;

    private LocalDate dateOfBirth;

    private String gender;

    private LocalDate confirmationDate;

    private Long branchId;

    private Long departmentId;

    private Long designationId;

    private Long managerId;
}
