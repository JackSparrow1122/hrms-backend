package com.gahrms.backend.modules.organization.repository;

import com.gahrms.backend.modules.organization.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DesignationRepository extends JpaRepository<Designation, Long> {
    List<Designation> findByCompanyId(Long companyId);
    List<Designation> findByCompanyIdAndActiveTrueOrderByLevelDesc(Long companyId);
    List<Designation> findByCompanyIdAndBranchIdAndActiveTrueOrderByLevelDesc(Long companyId, Long branchId);
    List<Designation> findByCompanyIdAndDepartmentIdAndActiveTrueOrderByLevelDesc(Long companyId, Long departmentId);
    List<Designation> findByCompanyIdAndDepartmentIdAndBranchIdAndActiveTrueOrderByLevelDesc(Long companyId, Long departmentId, Long branchId);
    Optional<Designation> findByCompanyIdAndCode(Long companyId, String code);
    boolean existsByCompanyIdAndCode(Long companyId, String code);
}
