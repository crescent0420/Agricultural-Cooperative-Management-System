package com.acms.security;

import com.acms.entity.AppUser;
import com.acms.repository.AppUserRepository;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class DatabaseBackedJwtConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AppUserRepository appUserRepository;

    public DatabaseBackedJwtConverter(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

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
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid access token."));

        final Object versionClaim = jwt.getClaims().get("token_version");

        if (!(versionClaim instanceof Number number)) {
            throw new BadCredentialsException("Invalid access token.");
        }

        long tokenVersion = number.longValue();

        if (tokenVersion != appUser.getTokenVersion()) {
            throw new BadCredentialsException("Invalid access token.");
        }

        var authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name())
        );

        return new JwtAuthenticationToken(
                jwt,
                authorities,
                appUser.getId().toString()
        );
    }
}