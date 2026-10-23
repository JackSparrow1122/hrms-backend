package com.gahrms.backend.modules.attendance.entity;

import com.gahrms.backend.modules.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "attendance_punches",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_punch_source", columnNames = {"source_system", "source_record_id"})
        }
)
public class AttendancePunch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "employee_code", nullable = false, length = 50)
    private String employeeCode;

    @Builder.Default
    @Column(name = "source_system", nullable = false, length = 50)
    private String sourceSystem = "ESSL_ETIMETRACK";

    @Column(name = "source_record_id", nullable = false)
    private Long sourceRecordId;

    @Column(name = "punch_time", nullable = false)
    private Instant punchTime;

    @Column(name = "punch_type", nullable = false, length = 10)
    private String punchType; // "IN" or "OUT"

    @Builder.Default
    @Column(name = "mapping_status", nullable = false, length = 20)
    private String mappingStatus = "MAPPED"; // "MAPPED" or "UNMAPPED"

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
