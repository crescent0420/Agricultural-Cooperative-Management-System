package com.acms.service;

import com.acms.common.BaseService;
import com.acms.dto.LoginRequest;
import com.acms.dto.LoginResponse;
import com.acms.entity.AppUser;
import com.acms.enums.AuditEventType;
import com.acms.exception.BusinessRuleException;
import com.acms.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService extends BaseService {

    // Only usernames that look like usernames are written to the audit log
    // (people sometimes type their password into the username box).
    private static final String SAFE_USERNAME = "[a-z0-9._-]{3,50}";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final AuditLogService auditLogService;
    private final AppUserRepository appUserRepository;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            // Checks the BCrypt hash and the enabled flag (see AppUserDetailsService).
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        } catch (AuthenticationException exception) {
            String username = request.username();
            if (username != null && username.matches(SAFE_USERNAME)) {
                auditLogService.record(AuditEventType.LOGIN_FAILURE, false, null, username,
                        "AUTHENTICATION", null, "Login failed", null);
            }
            throw exception;
        }

        Long userId = Long.valueOf(authentication.getName());
        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found."));

        // The password was right, but the email address has not been confirmed yet.
        if (!appUser.isEmailVerified()) {
            auditLogService.record(AuditEventType.LOGIN_FAILURE, false, appUser.getId(), appUser.getUsername(),
                    "AUTHENTICATION", appUser.getId(), "Email address not verified", null);
            throw new BusinessRuleException(
                    "Please confirm your email address first, using the link in your account email.");
        }

        JwtTokenService.IssuedToken issuedToken = jwtTokenService.issueToken(userId);
        auditLogService.record(AuditEventType.LOGIN_SUCCESS, true, appUser.getId(), appUser.getUsername(),
                "AUTHENTICATION", appUser.getId(),
                appUser.isMustChangePassword() ? "Login successful; password change required" : "Login successful",
                null);

        return new LoginResponse(issuedToken.value(), "Bearer", issuedToken.expiresInSeconds(),
                appUser.isMustChangePassword());
    }
}
