package com.gahrms.backend.modules.attendance.repository;

import com.gahrms.backend.modules.attendance.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    List<Holiday> findAllByOrderByHolidayDateAsc();
    Optional<Holiday> findByHolidayDate(LocalDate holidayDate);

    @Query("SELECT h FROM Holiday h WHERE h.holidayDate BETWEEN :startDate AND :endDate")
    List<Holiday> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
}