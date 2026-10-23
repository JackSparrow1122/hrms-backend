package com.gahrms.backend.modules.leave.repository;

import com.gahrms.backend.modules.leave.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    List<LeaveBalance> findByEmployeeIdAndYear(Long employeeId, Integer year);
    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeIdAndYear(Long employeeId, Long leaveTypeId, Integer year);
    Optional<LeaveBalance> findByEmployeeIdAndLeaveType_CodeIgnoreCaseAndYear(Long employeeId, String code, Integer year);
}
