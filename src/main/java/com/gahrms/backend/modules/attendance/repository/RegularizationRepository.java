package com.gahrms.backend.modules.attendance.repository;

import com.gahrms.backend.modules.attendance.entity.RegularizationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface RegularizationRepository extends JpaRepository<RegularizationRequest, Long> {

    List<RegularizationRequest> findByEmployee_IdOrderByCreatedAtDesc(Long employeeId);

    List<RegularizationRequest> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT COUNT(r) FROM RegularizationRequest r WHERE r.employee.id = :employeeId AND r.date = :date AND r.status IN ('PENDING', 'APPROVED')")
    long countActiveRequestsForDate(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);
}