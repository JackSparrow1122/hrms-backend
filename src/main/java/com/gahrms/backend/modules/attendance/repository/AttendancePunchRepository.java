package com.gahrms.backend.modules.attendance.repository;

import com.gahrms.backend.modules.attendance.entity.AttendancePunch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendancePunchRepository extends JpaRepository<AttendancePunch, Long>, JpaSpecificationExecutor<AttendancePunch> {

    boolean existsBySourceSystemAndSourceRecordId(String sourceSystem, Long sourceRecordId);

    boolean existsByEmployeeCodeAndPunchTime(String employeeCode, Instant punchTime);

    Optional<AttendancePunch> findBySourceSystemAndSourceRecordId(String sourceSystem, Long sourceRecordId);

    Page<AttendancePunch> findByEmployeeIdOrderByPunchTimeDesc(Long employeeId, Pageable pageable);

    Page<AttendancePunch> findByEmployeeCodeOrderByPunchTimeDesc(String employeeCode, Pageable pageable);

    List<AttendancePunch> findByEmployeeIdAndPunchTimeBetweenOrderByPunchTimeAsc(Long employeeId, Instant start, Instant end);

    Page<AttendancePunch> findByMappingStatusOrderByPunchTimeDesc(String mappingStatus, Pageable pageable);

    List<AttendancePunch> findByMappingStatus(String mappingStatus);

    long countByMappingStatus(String mappingStatus);

    List<AttendancePunch> findByPunchTimeBetweenOrderByPunchTimeAsc(Instant start, Instant end);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.employeeCode FROM AttendancePunch p WHERE p.employeeCode IS NOT NULL ORDER BY p.employeeCode ASC")
    List<String> findDistinctEmployeeCodes();

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.employeeCode FROM AttendancePunch p WHERE p.mappingStatus = 'UNMAPPED' AND p.employeeCode IS NOT NULL ORDER BY p.employeeCode ASC")
    List<String> findDistinctUnmappedCodes();
}
