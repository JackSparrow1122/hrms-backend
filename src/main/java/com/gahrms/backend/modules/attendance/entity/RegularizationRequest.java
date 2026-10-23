package com.gahrms.backend.modules.attendance.entity;

import com.gahrms.backend.modules.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "regularization_requests")
public class RegularizationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private Employee approver;


    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "proposed_punch_in", nullable = false)
    private Instant proposedPunchIn;

    @Column(name = "proposed_punch_out", nullable = false)
    private Instant proposedPunchOut;

    @Column(nullable = false, length = 500)
    private String reason;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "PENDING";

   
    @Column(name = "reviewer_remarks", length = 500)
    private String reviewerRemarks;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
