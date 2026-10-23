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
@Table(name = "attendance_records")
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "punch_in")
    private Instant punchIn;

    @Column(name = "punch_out")
    private Instant punchOut;

    @Builder.Default
    @Column(name = "worked_minutes", nullable = false)
    private Integer workedMinutes = 0;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "PRESENT";

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String source = "WEB";

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
