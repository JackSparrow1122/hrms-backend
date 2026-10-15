package com.gahrms.backend.modules.leave.entity;

import com.gahrms.backend.modules.employee.entity.Employee;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

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

    @Column(name = "total_allowance", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalAllowance = BigDecimal.ZERO;

    @Column(name = "used_count", nullable = false, precision = 5, scale = 2)
    private BigDecimal usedCount = BigDecimal.ZERO;

    @Column(name = "pending_count", nullable = false, precision = 5, scale = 2)
    private BigDecimal pendingCount = BigDecimal.ZERO;

    @Column(name = "remaining_count", nullable = false, precision = 5, scale = 2)
    private BigDecimal remainingCount = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public LeaveBalance() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public LeaveType getLeaveType() { return leaveType; }
    public void setLeaveType(LeaveType leaveType) { this.leaveType = leaveType; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public BigDecimal getTotalAllowance() { return totalAllowance; }
    public void setTotalAllowance(BigDecimal totalAllowance) { this.totalAllowance = totalAllowance; }

    public BigDecimal getUsedCount() { return usedCount; }
    public void setUsedCount(BigDecimal usedCount) { this.usedCount = usedCount; }

    public BigDecimal getPendingCount() { return pendingCount; }
    public void setPendingCount(BigDecimal pendingCount) { this.pendingCount = pendingCount; }

    public BigDecimal getRemainingCount() { return remainingCount; }
    public void setRemainingCount(BigDecimal remainingCount) { this.remainingCount = remainingCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
