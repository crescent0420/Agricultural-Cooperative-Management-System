package com.acms.service;

import com.acms.dto.AuditLogResponse;
import com.acms.dto.PageResponse;
import com.acms.entity.AuditEventType;
import com.acms.entity.AuditLog;
import com.acms.repository.AuditLogRepository;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "occurredAt", "eventType");

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
    public PageResponse<AuditLogResponse> findRecent(
            int page,
            int size,
            String sortBy,
            String direction) {

        Pageable pageable = Paging.of(
                page, size, sortBy, direction, SORTABLE_FIELDS);

        return PageResponse.from(
                auditLogRepository.findAll(pageable)
                        .map(AuditLogResponse::from));
    }
}