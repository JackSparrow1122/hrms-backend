package com.gahrms.backend.modules.identity.dto;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryDto {
    private Long id;
    private Long employeeId;
    private Long companyId;
    private String scopeType;
    private Long scopeBranchId;
    private Long scopeDepartmentId;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String employeeCode;
    private String designationName;
    private String departmentName;
    private String companyName;
    private Set<String> roles;
}
