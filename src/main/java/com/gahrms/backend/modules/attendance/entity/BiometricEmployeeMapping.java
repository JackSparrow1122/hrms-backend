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
        name = "biometric_employee_mappings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_biometric_emp_mapping", columnNames = {"source_system", "biometric_emp_code"})
        }
)
public class BiometricEmployeeMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "source_system", nullable = false, length = 50)
    private String sourceSystem = "ESSL_ETIMETRACK";

    @Column(name = "biometric_emp_code", nullable = false, length = 50)
    private String biometricEmpCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "notes", length = 255)
    private String notes;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
