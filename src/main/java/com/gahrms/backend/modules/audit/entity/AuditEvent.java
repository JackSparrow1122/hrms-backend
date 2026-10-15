package com.gahrms.backend.modules.audit.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_email", length = 150)
    private String actorEmail;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "before_state", columnDefinition = "TEXT")
    private String beforeState;

    @Column(name = "after_state", columnDefinition = "TEXT")
    private String afterState;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(nullable = false, updatable = false)
    private Instant timestamp = Instant.now();

    public AuditEvent() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final AuditEvent event = new AuditEvent();
        public Builder actorId(Long id) { event.actorId = id; return this; }
        public Builder actorEmail(String email) { event.actorEmail = email; return this; }
        public Builder action(String action) { event.action = action; return this; }
        public Builder resourceType(String type) { event.resourceType = type; return this; }
        public Builder resourceId(String id) { event.resourceId = id; return this; }
        public Builder beforeState(String s) { event.beforeState = s; return this; }
        public Builder afterState(String s) { event.afterState = s; return this; }
        public Builder ipAddress(String ip) { event.ipAddress = ip; return this; }
        public Builder userAgent(String ua) { event.userAgent = ua; return this; }
        public Builder status(String st) { event.status = st; return this; }
        public Builder correlationId(String cid) { event.correlationId = cid; return this; }
        public Builder timestamp(Instant ts) { event.timestamp = ts; return this; }
        public AuditEvent build() { return event; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }

    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getBeforeState() { return beforeState; }
    public void setBeforeState(String beforeState) { this.beforeState = beforeState; }

    public String getAfterState() { return afterState; }
    public void setAfterState(String afterState) { this.afterState = afterState; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
