  package com.acms.dto;

  import com.acms.enums.AuditEventType;
  import com.acms.entity.AuditLog;
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

      public static AuditLogResponse from(AuditLog log) {
          return new AuditLogResponse(
                  log.getId(), log.getOccurredAt(), log.getEventType(), log.isSuccess(),
                  log.getActorUserId(), log.getActorUsername(), log.getTargetType(),
                  log.getTargetId(), log.getDetails(), log.getIpAddress());
      }
  }