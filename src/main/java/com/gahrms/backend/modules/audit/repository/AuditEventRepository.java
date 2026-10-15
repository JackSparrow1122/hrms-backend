package com.gahrms.backend.modules.audit.repository;

import com.gahrms.backend.modules.audit.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    Page<AuditEvent> findByResourceTypeOrderByTimestampDesc(String resourceType, Pageable pageable);
}
