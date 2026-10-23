package com.gahrms.backend.modules.leave.entity;

import com.gahrms.backend.modules.organization.entity.Company;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "leave_types")
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(name = "annual_allowance", nullable = false, precision = 5, scale = 2)
    private BigDecimal annualAllowance;

    @Builder.Default
    @Column(name = "is_paid", nullable = false)
    private boolean paid = true;

    @Builder.Default
    @Column(name = "requires_document", nullable = false)
    private boolean requiresDocument = false;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
