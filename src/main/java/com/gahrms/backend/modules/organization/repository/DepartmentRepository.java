package com.gahrms.backend.modules.organization.repository;

import com.gahrms.backend.modules.organization.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByCompanyId(Long companyId);
    List<Department> findByCompanyIdAndActiveTrue(Long companyId);
    List<Department> findByCompanyIdAndBranchId(Long companyId, Long branchId);
    List<Department> findByCompanyIdAndBranchIdAndActiveTrue(Long companyId, Long branchId);
    Optional<Department> findByCompanyIdAndCode(Long companyId, String code);
    boolean existsByCompanyIdAndCode(Long companyId, String code);
    boolean existsByParentDepartmentId(Long parentDepartmentId);
}
