package com.acms.service;

import com.acms.dto.AuditLogResponse;
import com.acms.dto.PageResponse;
import com.acms.entity.AuditEventType;
import com.acms.entity.AuditLog;
import com.acms.repository.AuditLogRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "occurredAt", "eventType");

    private final AuditLogRepository auditLogRepository;

    /**
     * For ATTEMPTS and FAILURES (failed login, access denied, wrong password...).
     * REQUIRES_NEW = its own transaction, so the record survives even when the caller rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEventType eventType, boolean success, Long actorUserId, String actorUsername,
                       String targetType, Long targetId, String details, String ipAddress) {
        auditLogRepository.save(new AuditLog(eventType, success, actorUserId, actorUsername,
                targetType, targetId, details, ipAddress));
    }

    /**
     * For SUCCESSFUL CHANGES (user created, role changed, password changed...).
     * MANDATORY = join the caller's transaction, so the audit row is saved only if the change itself
     * is saved. It fails loudly if someone calls it outside a transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordInCurrentTransaction(AuditEventType eventType, boolean success, Long actorUserId,
                                           String actorUsername, String targetType, Long targetId,
                                           String details, String ipAddress) {
        auditLogRepository.save(new AuditLog(eventType, success, actorUserId, actorUsername,
                targetType, targetId, details, ipAddress));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> findRecent(int page, int size, String sortBy, String direction) {
        Pageable pageable = Paging.of(page, size, sortBy, direction, SORTABLE_FIELDS);
        return PageResponse.from(auditLogRepository.findAll(pageable).map(AuditLogResponse::from));
    }
}
