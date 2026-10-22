
package com.acms.dto;

import com.acms.entity.AuditEventType;
import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Instant occurredAt,
        AuditEventType eventType,
        boolean success,
        Long actorUserId,
        String actorUsername,
        String targetType,
        Long targetId,
        String details,
        String ipAddress
) {
}
