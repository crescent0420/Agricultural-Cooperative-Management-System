
package com.acms.service;

import com.acms.dto.LoginRequest;
import com.acms.dto.LoginResponse;
import com.acms.entity.AuditEventType;
import com.acms.entity.AppUser;
import com.acms.repository.AppUserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final AuditLogService auditLogService;
    private final AppUserRepository appUserRepository;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            AuditLogService auditLogService,
            AppUserRepository appUserRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.auditLogService = auditLogService;
        this.appUserRepository = appUserRepository;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.username(),
                            request.password()
                    )
            );
        } catch (AuthenticationException exception) {
            String username = request.username();

            if (username != null
                    && username.matches("[a-z0-9._-]{3,50}")) {
                auditLogService.record(
                        AuditEventType.LOGIN_FAILURE,
                        false,
                        null,
                        username,
                        "AUTHENTICATION",
                        null,
                        "Login failed",
                        null
                );
            }

            throw exception;
        }

        Long userId = Long.valueOf(authentication.getName());

        JwtTokenService.IssuedToken issuedToken =
                jwtTokenService.issueToken(userId);

        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user was not found."
                        ));

        auditLogService.record(
                AuditEventType.LOGIN_SUCCESS,
                true,
                appUser.getId(),
                appUser.getUsername(),
                "AUTHENTICATION",
                appUser.getId(),
                "Login successful",
                null
        );

        return new LoginResponse(
                issuedToken.value(),
                "Bearer",
                issuedToken.expiresInSeconds()
        );
    }
}
