package com.acms.service;

import com.acms.common.BaseService;
import com.acms.common.MessageResponse;
import com.acms.dto.RegisterRequest;
import com.acms.entity.AppUser;
import com.acms.entity.District;
import com.acms.entity.EmailVerificationToken;
import com.acms.entity.Farmer;
import com.acms.enums.AuditEventType;
import com.acms.enums.Role;
import com.acms.exception.BadRequestException;
import com.acms.exception.ConflictException;
import com.acms.mail.CredentialsIssuedEvent;
import com.acms.repository.AppUserRepository;
import com.acms.repository.EmailVerificationTokenRepository;
import com.acms.security.SecureTokens;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Creating accounts, issuing one-time passwords and verifying email addresses. */
@Service
@RequiredArgsConstructor
public class AccountOnboardingService extends BaseService {

    private static final Duration VERIFICATION_LINK_VALIDITY = Duration.ofHours(24);
    private static final String INVALID_LINK = "The verification link is invalid, already used or expired.";

    private final AppUserRepository appUserRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogService auditLogService;

    /** Public sign-up. The very first account becomes System Administrator; everyone after that is a FARMER. */
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        // Lock first, then count. Without the lock, two registrations on an empty database could
        // both count 0 users and both become administrator.
        appUserRepository.lockRegistrations();

        String username = request.username();
        String email = normaliseEmail(request.email());
        if (appUserRepository.existsByUsername(username)) {
            throw new ConflictException("Username is already in use.");
        }
        if (appUserRepository.existsByEmail(email)) {
            throw new ConflictException("Email address is already in use.");
        }

        boolean firstUser = appUserRepository.count() == 0;
        Role role = firstUser ? Role.SYSTEM_ADMIN : Role.FARMER;

        AppUser saved = createAccount(username, email, role, null, null, firstUser);
        auditLogService.recordInCurrentTransaction(AuditEventType.USER_REGISTERED, true,
                saved.getId(), saved.getUsername(), "USER", saved.getId(),
                "Self-registered with role " + role, null);

        return new MessageResponse(
                "Account created. Check your email for a verification link and a one-time password.");
    }

    /**
     * Saves a new account with a random one-time password and schedules the account email.
     * The caller has already checked the role and its district or farmer scope.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AppUser createAccount(String username, String email, Role role,
                                 District district, Farmer farmer, boolean firstAdministrator) {
        String temporaryPassword = SecureTokens.temporaryPassword();
        AppUser user = new AppUser(username, email, passwordEncoder.encode(temporaryPassword), role);
        if (district != null) {
            user.assignDistrict(district);
        } else if (farmer != null) {
            user.assignFarmer(farmer);
        }
        AppUser saved = appUserRepository.save(user);

        String verificationToken = newVerificationToken(saved);
        // Sent by AccountEmailSender only AFTER this transaction commits.
        eventPublisher.publishEvent(new CredentialsIssuedEvent(saved.getUsername(), saved.getEmail(),
                temporaryPassword, verificationToken, firstAdministrator));
        return saved;
    }

    /** New one-time password (and a new verification link if the email is not verified yet). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reissueCredentials(AppUser user) {
        String temporaryPassword = SecureTokens.temporaryPassword();
        user.resetToTemporaryPassword(passwordEncoder.encode(temporaryPassword));
        appUserRepository.save(user);

        String verificationToken = null;
        if (!user.isEmailVerified()) {
            tokenRepository.invalidateUnusedForUser(user.getId(), Instant.now());   // old links stop working
            verificationToken = newVerificationToken(user);
        }
        eventPublisher.publishEvent(new CredentialsIssuedEvent(user.getUsername(), user.getEmail(),
                temporaryPassword, verificationToken, false));
    }

    /** The link in the email. Works once, for 24 hours. */
    @Transactional
    public MessageResponse verifyEmail(String rawToken) {
        Instant now = Instant.now();
        Optional<EmailVerificationToken> found = (rawToken == null || rawToken.isBlank())
                ? Optional.empty()
                : tokenRepository.findByTokenHash(SecureTokens.sha256Hex(rawToken.trim()));

        if (found.isEmpty() || !found.get().isUsable(now)) {
            Long userId = found.map(token -> token.getUser().getId()).orElse(null);
            auditLogService.record(AuditEventType.EMAIL_VERIFICATION_FAILED, false, userId, null,
                    "USER", userId, "Invalid, already used or expired verification link", null);
            throw new BadRequestException(INVALID_LINK);
        }

        EmailVerificationToken token = found.get();
        token.markUsed(now);
        AppUser user = token.getUser();
        user.markEmailVerified();

        auditLogService.recordInCurrentTransaction(AuditEventType.EMAIL_VERIFIED, true,
                user.getId(), user.getUsername(), "USER", user.getId(), "Email address verified", null);
        return new MessageResponse(
                "Your email address is verified. You can now log in with the one-time password from the email.");
    }

    public static String normaliseEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String newVerificationToken(AppUser user) {
        String rawToken = SecureTokens.urlToken();
        tokenRepository.save(new EmailVerificationToken(user, SecureTokens.sha256Hex(rawToken),
                Instant.now().plus(VERIFICATION_LINK_VALIDITY)));
        return rawToken;   // only the hash is stored; the raw token goes only into the email
    }
}
