package com.gahrms.backend.modules.organization.repository;

import com.gahrms.backend.modules.organization.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {
    Optional<Company> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByNameIgnoreCase(String name);
}
