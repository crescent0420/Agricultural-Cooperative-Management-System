package com.acms.security;

import com.acms.entity.AppUser;
import com.acms.repository.AppUserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseBackedJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    /**
     * Given INSTEAD of the real role while the user still has to replace the one-time password.
     * No business endpoint accepts it; only /api/auth/me and /api/auth/change-password do.
     */
    public static final String PASSWORD_CHANGE_REQUIRED = "PASSWORD_CHANGE_REQUIRED";

    private final AppUserRepository appUserRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        final Long userId;
        try {
            userId = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException | NullPointerException ex) {
            throw new BadCredentialsException("Invalid access token.");
        }

        AppUser appUser = appUserRepository.findById(userId)
                .filter(AppUser::isEnabled)
                .filter(AppUser::isEmailVerified)
                .orElseThrow(() -> new BadCredentialsException("Invalid access token."));

        Object versionClaim = jwt.getClaims().get("token_version");
        if (!(versionClaim instanceof Number number) || number.longValue() != appUser.getTokenVersion()) {
            throw new BadCredentialsException("Invalid access token.");
        }

        String authority = appUser.isMustChangePassword()
                ? PASSWORD_CHANGE_REQUIRED
                : appUser.getRole().name();

        return new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_" + authority)),
                appUser.getId().toString());
    }
}
