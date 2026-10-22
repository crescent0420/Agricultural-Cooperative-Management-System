package com.acms.service;

import com.acms.dto.AuditLogResponse;
import com.acms.entity.AuditEventType;
import com.acms.entity.AuditLog;
import com.acms.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            AuditEventType eventType,
            boolean success,
            Long actorUserId,
            String actorUsername,
            String targetType,
            Long targetId,
            String details,
            String ipAddress) {

        AuditLog auditLog = new AuditLog(
                eventType,
                success,
                actorUserId,
                actorUsername,
                targetType,
                targetId,
                details,
                ipAddress
        );

        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable)
                .map(log -> new AuditLogResponse(
                        log.getId(),
                        log.getOccurredAt(),
                        log.getEventType(),
                        log.isSuccess(),
                        log.getActorUserId(),
                        log.getActorUsername(),
                        log.getTargetType(),
                        log.getTargetId(),
                        log.getDetails(),
                        log.getIpAddress()
                ));
    }
}