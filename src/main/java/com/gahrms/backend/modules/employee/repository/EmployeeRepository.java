package com.gahrms.backend.modules.employee.repository;

import com.gahrms.backend.modules.employee.entity.Employee;
import com.gahrms.backend.modules.employee.entity.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    Optional<Employee> findByEmployeeCodeIgnoreCase(String employeeCode);

    Optional<Employee> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmployeeCode(String employeeCode);

    List<Employee> findByCompanyId(Long companyId);

    List<Employee> findByCompanyIdAndStatus(Long companyId, EmploymentStatus status);

    List<Employee> findByManagerId(Long managerId);

    long countByCompanyId(Long companyId);

    long countByBranchId(Long branchId);

    long countByDepartmentId(Long departmentId);

    long countByDesignationId(Long designationId);

    @Query("SELECT e FROM Employee e WHERE e.company.id = :companyId AND (:excludeId IS NULL OR e.id != :excludeId) AND e.status = 'ACTIVE' ORDER BY e.firstName ASC")
    List<Employee> findEligibleManagers(@Param("companyId") Long companyId, @Param("excludeId") Long excludeId);
}
