package com.gahrms.backend.modules.attendance.repository;

import com.gahrms.backend.modules.attendance.entity.AttendanceSyncState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttendanceSyncStateRepository extends JpaRepository<AttendanceSyncState, Long> {

    Optional<AttendanceSyncState> findBySourceSystem(String sourceSystem);
}
