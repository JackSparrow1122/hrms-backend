package com.gahrms.backend.modules.audit.service;

import com.gahrms.backend.common.util.SecurityUtils;
import com.gahrms.backend.modules.audit.entity.AuditEvent;
import com.gahrms.backend.modules.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;

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
