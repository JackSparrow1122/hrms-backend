package com.gahrms.backend.modules.employee.dto;

import com.gahrms.backend.modules.employee.entity.EmploymentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEmployeeRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String phoneNumber;

    private LocalDate dateOfBirth;

    private String gender;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    private LocalDate confirmationDate;

    @Builder.Default
    private EmploymentStatus status = EmploymentStatus.ACTIVE;

    @NotNull(message = "Company ID is required")
    private Long companyId;

    private Long branchId;

    private Long departmentId;

    private Long designationId;

    private Long managerId;

    @Builder.Default
    private boolean createLoginAccount = true;

    private String initialPassword;
}
