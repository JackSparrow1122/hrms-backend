package com.gahrms.backend.modules.leave.repository;

import com.gahrms.backend.modules.leave.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
    List<LeaveRequest> findByApproverIdOrderByCreatedAtDesc(Long approverId);
    List<LeaveRequest> findByApproverIdAndStatusOrderByCreatedAtDesc(Long approverId, String status);
    List<LeaveRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<LeaveRequest> findAllByOrderByCreatedAtDesc();

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.company.id = :companyId ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.company.id = :companyId AND UPPER(lr.status) = UPPER(:status) ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findByCompanyIdAndStatus(@Param("companyId") Long companyId, @Param("status") String status);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.id = :employeeId AND lr.status = 'APPROVED' AND lr.startDate <= :endDate AND lr.endDate >= :startDate")
    List<LeaveRequest> findApprovedLeavesInRange(@Param("employeeId") Long employeeId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT lr FROM LeaveRequest lr WHERE UPPER(lr.status) = 'APPROVED' AND lr.startDate <= :endDate AND lr.endDate >= :startDate")
    List<LeaveRequest> findAllApprovedLeavesInRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT lr FROM LeaveRequest lr WHERE (lr.approver.id = :approverId OR lr.employee.manager.id = :approverId) ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findByApproverOrManager(@Param("approverId") Long approverId);

    @Query("SELECT lr FROM LeaveRequest lr WHERE (lr.approver.id = :approverId OR lr.employee.manager.id = :approverId) AND UPPER(lr.status) = UPPER(:status) ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findByApproverOrManagerAndStatus(@Param("approverId") Long approverId, @Param("status") String status);

    @Query("SELECT COUNT(lr) FROM LeaveRequest lr WHERE lr.employee.id = :employeeId " +
           "AND UPPER(lr.status) IN ('PENDING', 'APPROVED') " +
           "AND lr.startDate <= :endDate AND lr.endDate >= :startDate")
    long countOverlappingLeave(@Param("employeeId") Long employeeId,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate);
}
