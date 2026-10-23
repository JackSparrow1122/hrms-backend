package com.gahrms.backend.modules.employee.dto;

import com.gahrms.backend.modules.employee.entity.EmploymentStatus;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponse {

    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String gender;
    private LocalDate joiningDate;
    private LocalDate confirmationDate;
    private EmploymentStatus status;

    // Organization mapping
    private Long companyId;
    private String companyName;
    private Long branchId;
    private String branchName;
    private Long departmentId;
    private String departmentName;
    private Long designationId;
    private String designationName;

    // Manager mapping
    private Long managerId;
    private String managerName;
    private String managerCode;

    // Login account status
    private Long userId;
    private boolean hasLoginAccount;

    private Instant createdAt;
    private Instant updatedAt;
}
