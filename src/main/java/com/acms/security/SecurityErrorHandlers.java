package com.acms.security;

import com.acms.entity.AppUser;
import com.acms.entity.AuditEventType;
import com.acms.exception.ApiError;
import com.acms.repository.AppUserRepository;
import com.acms.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityErrorHandlers {

    private static final Logger log =
            LoggerFactory.getLogger(SecurityErrorHandlers.class);

    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final AppUserRepository appUserRepository;

    public SecurityErrorHandlers(
            ObjectMapper objectMapper,
            AuditLogService auditLogService,
            AppUserRepository appUserRepository) {
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
        this.appUserRepository = appUserRepository;
    }

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) ->
                writeError(
                        request,
                        response,
                        HttpStatus.UNAUTHORIZED,
                        "Authentication is required or the access token is invalid."
                );
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> {
            recordAccessDenied(request);

            writeError(
                    request,
                    response,
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to perform this action."
            );
        };
    }

    private void recordAccessDenied(HttpServletRequest request) {
        try {
            Long actorUserId = null;
            String actorUsername = null;

            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null
                    && authentication.isAuthenticated()
                    && authentication.getName() != null) {
                try {
                    Long userId = Long.valueOf(authentication.getName());

                    AppUser user = appUserRepository.findById(userId)
                            .filter(AppUser::isEnabled)
                            .orElse(null);

                    if (user != null) {
                        actorUserId = user.getId();
                        actorUsername = user.getUsername();
                    }
                } catch (NumberFormatException ignored) {
                    // Anonymous or non-database authentication has no known user ID.
                }
            }

            String details = request.getMethod()
                    + " "
                    + request.getRequestURI()
                    + " denied";

            if (details.length() > 500) {
                details = details.substring(0, 500);
            }

            String ipAddress = request.getRemoteAddr();
            if (ipAddress != null && ipAddress.length() > 45) {
                ipAddress = ipAddress.substring(0, 45);
            }

            auditLogService.record(
                    AuditEventType.ACCESS_DENIED,
                    false,
                    actorUserId,
                    actorUsername,
                    "HTTP_REQUEST",
                    null,
                    details,
                    ipAddress
            );
        } catch (RuntimeException auditException) {
            log.warn("Failed to record access-denied audit event for {} {}",
                    request.getMethod(), request.getRequestURI(), auditException);
        }
    }

    private void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String message) throws IOException {

        ApiError body = new ApiError(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                List.of()
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}