package com.acms.exception;

import com.acms.common.ApiError;
import com.acms.common.ApiException;
import com.acms.common.ApiResponse;
import com.acms.entity.AppUser;
import com.acms.entity.AuditEventType;
import com.acms.repository.AppUserRepository;
import com.acms.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Turns every exception into the same ApiResponse envelope, with success = false. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuditLogService auditLogService;
    private final AppUserRepository appUserRepository;

    public GlobalExceptionHandler(AuditLogService auditLogService, AppUserRepository appUserRepository) {
        this.auditLogService = auditLogService;
        this.appUserRepository = appUserRepository;
    }

    // 404, 409, 422, 400: all our own exceptions carry their status (see ApiException and its subclasses)
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request, List.of());
    }

    // 400: a field failed @Valid (@NotBlank, @Size, ...)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest request) {
        List<ApiError.FieldIssue> issues = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldIssue(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, issues);
    }

    // 400: the JSON body is broken or has the wrong types
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex,
                                                              HttpServletRequest request) {
        log.warn("Unreadable request body on {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body.", request, List.of());
    }

    // 409: the database itself refused (unique or foreign key constraint). Never show the SQL message.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                 HttpServletRequest request) {
        log.warn("Database constraint violation on {}: {}", request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "The request conflicts with existing data.", request, List.of());
    }

    // 400: a URL or query parameter has the wrong type
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "Parameter '" + ex.getName() + "' has an invalid value.", request, List.of());
    }

    // 409: another request changed the record concurrently
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.CONFLICT,
                "This record was changed by another request. Reload it and try again.", request, List.of());
    }

    // 403: role, district or ownership check failed
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex,
                                                                HttpServletRequest request) {
        try {
            Long actorUserId = null;
            String actorUsername = null;

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null) {
                try {
                    Long userId = Long.valueOf(authentication.getName());
                    AppUser user = appUserRepository.findById(userId).filter(AppUser::isEnabled).orElse(null);
                    if (user != null) {
                        actorUserId = user.getId();
                        actorUsername = user.getUsername();
                    }
                } catch (NumberFormatException ignored) {
                    // The authenticated principal is not a database user ID.
                }
            }

            String details = request.getMethod() + " " + request.getRequestURI() + " denied";
            if (details.length() > 500) {
                details = details.substring(0, 500);
            }
            String ipAddress = request.getRemoteAddr();
            if (ipAddress != null && ipAddress.length() > 45) {
                ipAddress = ipAddress.substring(0, 45);
            }

            auditLogService.record(AuditEventType.ACCESS_DENIED, false, actorUserId, actorUsername,
                    "HTTP_REQUEST", null, details, ipAddress);
        } catch (RuntimeException auditException) {
            log.warn("Failed to record access-denied audit event for {} {}",
                    request.getMethod(), request.getRequestURI(), auditException);
        }

        return build(HttpStatus.FORBIDDEN, "You do not have permission to perform this action.", request, List.of());
    }

    // 401: login failed or the account cannot authenticate
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid username or password.", request, List.of());
    }

    // Everything else
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAny(Exception ex, HttpServletRequest request) {
        // Spring's own web exceptions (404 unknown URL, 405 wrong method, missing parameter...) know their status.
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.resolve(errorResponse.getStatusCode().value());
            if (status != null) {
                return build(status, status.getReasonPhrase(), request, List.of());
            }
        }
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request, List.of());
    }

    /** The same error envelope for every case. Also used by SecurityErrorHandlers for 401/403 from the filters. */
    public static ApiResponse<Void> errorBody(HttpStatus status, String message, String path,
                                              List<ApiError.FieldIssue> issues) {
        return ApiResponse.failure(message, new ApiError(status.value(), status.getReasonPhrase(), path, issues));
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String message, HttpServletRequest request,
                                                    List<ApiError.FieldIssue> issues) {
        return ResponseEntity.status(status).body(errorBody(status, message, request.getRequestURI(), issues));
    }
}
