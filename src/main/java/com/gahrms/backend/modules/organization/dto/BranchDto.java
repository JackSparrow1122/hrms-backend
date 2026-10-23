package com.gahrms.backend.modules.organization.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchDto {
    private Long id;
    private Long companyId;
    private String name;
    private String code;
    private String city;
    private String state;
    private String country;
    private boolean headquarters;
    private Long hrAdminId;
    private String hrAdminName;
    private String hrAdminEmail;
    private long employeeCount;
    private Instant createdAt;
}
