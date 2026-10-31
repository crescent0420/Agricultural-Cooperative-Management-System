package com.acms.config;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
@Configuration
public class JwtConfig {
    @Bean
    public JwtEncoder jwtEncoder(JwtProperties properties) {
        SecretKey secretKey = jwtSecretKey(properties);
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }
    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties) {
        SecretKey secretKey = jwtSecretKey(properties);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(properties.issuer())
        );
        return decoder;
    }
    private SecretKey jwtSecretKey(JwtProperties properties) {
        String secret = properties.secret();
        if (secret == null
                || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "ACMS_JWT_SECRET must contain at least 32 UTF-8 bytes."
            );
        }
        return new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
    }
}