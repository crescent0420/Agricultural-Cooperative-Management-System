
package com.acms.entity;

import com.acms.common.BaseEntity;
import com.acms.enums.AuditEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "audit_log")
public class AuditLog extends BaseEntity {

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50, updatable = false)
    private AuditEventType eventType;

    @Column(nullable = false, updatable = false)
    private boolean success;

    @Column(name = "actor_user_id", updatable = false)
    private Long actorUserId;

    @Column(name = "actor_username", length = 100, updatable = false)
    private String actorUsername;

    @Column(name = "target_type", length = 50, updatable = false)
    private String targetType;

    @Column(name = "target_id", updatable = false)
    private Long targetId;

    @Column(length = 500, updatable = false)
    private String details;

    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    protected AuditLog() {
    }

    public AuditLog(
            AuditEventType eventType,
            boolean success,
            Long actorUserId,
            String actorUsername,
            String targetType,
            Long targetId,
            String details,
            String ipAddress) {
        this.occurredAt = Instant.now();
        this.eventType = eventType;
        this.success = success;
        this.actorUserId = actorUserId;
        this.actorUsername = actorUsername;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
        this.ipAddress = ipAddress;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public boolean isSuccess() {
        return success;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getDetails() {
        return details;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
