package com.acms.service;

import com.acms.config.JwtProperties;
import com.acms.entity.AppUser;
import com.acms.repository.AppUserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final AppUserRepository appUserRepository;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            JwtProperties jwtProperties,
            AppUserRepository appUserRepository) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.appUserRepository = appUserRepository;
    }

    public IssuedToken issueToken(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(
                Math.multiplyExact(jwtProperties.ttlMinutes(), 60L)
        );

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(userId.toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("token_version", user.getTokenVersion())
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        org.springframework.security.oauth2.jwt.JwsHeader
                                .with(MacAlgorithm.HS256)
                                .build(),
                        claims
                )
        ).getTokenValue();

        return new IssuedToken(
                token,
                Math.max(
                        0L,
                        expiresAt.getEpochSecond() - issuedAt.getEpochSecond()
                )
        );
    }

    public record IssuedToken(String value, long expiresInSeconds) {
    }
}