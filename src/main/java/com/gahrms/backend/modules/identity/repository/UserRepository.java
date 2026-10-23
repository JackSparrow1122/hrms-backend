package com.gahrms.backend.modules.identity.repository;

import com.gahrms.backend.modules.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByEmployeeId(Long employeeId);
    List<User> findByCompanyId(Long companyId);
    List<User> findByCompanyIdAndScopeBranchId(Long companyId, Long scopeBranchId);
    Optional<User> findFirstByScopeBranchId(Long scopeBranchId);
    long countByCompanyId(Long companyId);

    @org.springframework.data.jpa.repository.Query("SELECT u.employeeId FROM User u JOIN u.roles r WHERE u.companyId = :companyId AND r.name = 'ROLE_TENANT_ADMIN' AND u.employeeId IS NOT NULL")
    List<Long> findTenantAdminEmployeeIds(@org.springframework.data.repository.query.Param("companyId") Long companyId);
}
