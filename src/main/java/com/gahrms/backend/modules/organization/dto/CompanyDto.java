package com.gahrms.backend.modules.organization.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyDto {
    private Long id;
    private String name;
    private String code;
    private String registrationNumber;
    private String taxId;
    private String timezone;
    private String currency;
    private Instant createdAt;
}
