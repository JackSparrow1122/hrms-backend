package com.gahrms.backend.modules.organization.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentDto {
    private Long id;
    private Long companyId;
    private Long parentDepartmentId;
    private String parentDepartmentName;
    private String name;
    private String code;
    private Long branchId;
    private String branchName;
    private boolean active;
    private Instant createdAt;
}
