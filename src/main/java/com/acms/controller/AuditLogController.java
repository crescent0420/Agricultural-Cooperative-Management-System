package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.config.ApiTags;
import com.acms.dto.AuditLogResponse;
import com.acms.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = ApiTags.AUDIT_AND_PERMISSIONS)
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController extends BaseController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Operation(
            summary = "Read the audit log",
            description = """
                    Security audit log (logins, refusals, changes).

                    - **Permission:** `AUDIT_LOG_READ` (Admin)
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, occurredAt, eventType; default occurredAt), `direction` (asc or desc; default desc)
                    - **Success:** 200
                    """)
    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_LOG_READ')")   // second check, after the URL rule in SecurityConfig
    public ApiResponse<PagedResponse<AuditLogResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ok(auditLogService.findRecent(page, size, sortBy, direction));
    }
}
