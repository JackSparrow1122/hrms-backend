package com.gahrms.backend.modules.attendance.repository;

import com.gahrms.backend.modules.attendance.entity.BiometricEmployeeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BiometricEmployeeMappingRepository extends JpaRepository<BiometricEmployeeMapping, Long> {

    Optional<BiometricEmployeeMapping> findBySourceSystemAndBiometricEmpCode(String sourceSystem, String biometricEmpCode);

    boolean existsBySourceSystemAndBiometricEmpCode(String sourceSystem, String biometricEmpCode);

    List<BiometricEmployeeMapping> findBySourceSystem(String sourceSystem);

    List<BiometricEmployeeMapping> findByEmployeeId(Long employeeId);
}
