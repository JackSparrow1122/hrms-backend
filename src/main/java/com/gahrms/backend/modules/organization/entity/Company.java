package com.gahrms.backend.modules.organization.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(name = "tax_id", length = 100)
    private String taxId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Builder.Default
    @Column(name = "subscription_plan", length = 50)
    private String subscriptionPlan = "ENTERPRISE";

    @Builder.Default
    @Column(name = "max_employees")
    private Integer maxEmployees = 500;

    @Builder.Default
    @Column(name = "max_branches")
    private Integer maxBranches = 5;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "domain", length = 100)
    private String domain;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String timezone = "Asia/Kolkata";

    @Builder.Default
    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
