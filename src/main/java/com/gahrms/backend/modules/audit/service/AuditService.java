package com.gahrms.backend.modules.audit.service;

import com.gahrms.backend.common.util.SecurityUtils;
import com.gahrms.backend.modules.audit.entity.AuditEvent;
import com.gahrms.backend.modules.audit.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Async
    public void recordEvent(String action, String resourceType, String resourceId, String beforeState, String afterState, String status) {
        try {
            AuditEvent event = AuditEvent.builder()
                    .actorEmail(SecurityUtils.getCurrentUserEmail().orElse("ANONYMOUS"))
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .beforeState(beforeState)
                    .afterState(afterState)
                    .status(status)
                    .correlationId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .build();

            auditEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to persist audit event for action {}: {}", action, e.getMessage());
        }
    }
}
