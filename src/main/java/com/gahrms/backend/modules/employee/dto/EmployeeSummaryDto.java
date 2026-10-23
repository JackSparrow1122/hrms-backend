package com.gahrms.backend.modules.employee.dto;

import com.gahrms.backend.modules.employee.entity.EmploymentStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSummaryDto {

    private Long id;
    private String employeeCode;
    private String fullName;
    private String email;
    private String designationName;
    private String departmentName;
    private EmploymentStatus status;
}
