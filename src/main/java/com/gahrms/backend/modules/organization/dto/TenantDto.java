package com.gahrms.backend.modules.organization.dto;

import com.gahrms.backend.modules.organization.entity.TenantStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantDto {
    private Long id;
    private String name;
    private String code;
    private String registrationNumber;
    private String taxId;
    private String timezone;
    private String currency;
    private TenantStatus status;
    private String subscriptionPlan;
    private Integer maxEmployees;
    private Integer maxBranches;
    private String contactEmail;
    private String contactPhone;
    private String domain;
    private long totalBranches;
    private long totalDepartments;
    private long totalEmployees;
    private String adminEmail;
    private String adminName;
    private Instant createdAt;
    private Instant updatedAt;
}
