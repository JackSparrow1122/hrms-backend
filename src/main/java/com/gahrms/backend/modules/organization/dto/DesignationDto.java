package com.gahrms.backend.modules.organization.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignationDto {
    private Long id;
    private Long companyId;
    private Long departmentId;
    private String departmentName;
    private String name;
    private String code;
    private Long branchId;
    private String branchName;
    private Integer level;
    private boolean active;
    private Instant createdAt;
}
