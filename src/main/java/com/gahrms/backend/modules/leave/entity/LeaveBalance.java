package com.gahrms.backend.modules.leave.entity;

import com.gahrms.backend.modules.employee.entity.Employee;
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
@Table(name = "leave_balances")
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(nullable = false)
    private Integer year;

    @Builder.Default
    @Column(name = "total_allowance", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalAllowance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "used_count", nullable = false, precision = 5, scale = 2)
    private BigDecimal usedCount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "pending_count", nullable = false, precision = 5, scale = 2)
    private BigDecimal pendingCount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "remaining_count", nullable = false, precision = 5, scale = 2)
    private BigDecimal remainingCount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
