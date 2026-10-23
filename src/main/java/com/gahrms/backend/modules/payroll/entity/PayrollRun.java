package com.gahrms.backend.modules.payroll.entity;

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
@Table(name = "payroll_runs")
public class PayrollRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_period_id", nullable = false)
    private PayrollPeriod payrollPeriod;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "DRAFT";

    @Builder.Default
    @Column(name = "total_employees", nullable = false)
    private Integer totalEmployees = 0;

    @Builder.Default
    @Column(name = "gross_payout", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossPayout = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_deductions", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "net_payout", nullable = false, precision = 14, scale = 2)
    private BigDecimal netPayout = BigDecimal.ZERO;

    @Column(name = "calculated_at")
    private Instant calculatedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
