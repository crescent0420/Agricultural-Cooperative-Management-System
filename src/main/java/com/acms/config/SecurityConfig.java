package com.acms.config;

import com.acms.security.DatabaseBackedJwtConverter;
import com.acms.security.SecurityErrorHandlers;
import com.acms.service.AppUserDetailsService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            AppUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            DaoAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DatabaseBackedJwtConverter jwtConverter,
            SecurityErrorHandlers errorHandlers) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(
                    errorHandlers.authenticationEntryPoint())
                .accessDeniedHandler(
                    errorHandlers.accessDeniedHandler())
            )
            .authorizeHttpRequests(auth -> auth

                // Public endpoints
                .requestMatchers(
                    HttpMethod.POST, "/api/auth/login"
                ).permitAll()
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                
                .requestMatchers("/error").permitAll()

                // Every authenticated user can read reference data.
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/districts",
                    "/api/districts/**",
                    "/api/cooperatives",
                    "/api/cooperatives/**",
                    "/api/trainings",
                    "/api/trainings/**",
                    "/api/programmes",
                    "/api/programmes/**"
                ).authenticated()

                // Only union registrars can modify reference data.
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/districts",
                    "/api/cooperatives",
                    "/api/trainings",
                    "/api/programmes",
                    "/api/programmes/*/prerequisites"
                ).hasRole("UNION_REGISTRAR")

                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/districts/**",
                    "/api/cooperatives/**",
                    "/api/trainings/**",
                    "/api/programmes/**"
                ).hasRole("UNION_REGISTRAR")

                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/programmes/*/prerequisites/*"
                ).hasRole("UNION_REGISTRAR")

                // Farmer summaries: staff roles that need summary access.
                .requestMatchers(
                    HttpMethod.GET, "/api/farmers"
                ).hasAnyRole(
                    "UNION_REGISTRAR",
                    "FIELD_OFFICER",
                    "DISTRICT_AGRONOMIST",
                    "FINANCE_OFFICER",
                    "SYSTEM_ADMIN"
                )

                // Farmer details: no finance or system-admin access.
                .requestMatchers(
                    HttpMethod.GET, "/api/farmers/*"
                ).hasAnyRole(
                    "FARMER",
                    "UNION_REGISTRAR",
                    "FIELD_OFFICER",
                    "DISTRICT_AGRONOMIST"
                )

                // Farmer registration and changes: registrar only.
                .requestMatchers(
                    HttpMethod.POST, "/api/farmers"
                ).hasRole("UNION_REGISTRAR")

                .requestMatchers(
                    HttpMethod.PUT, "/api/farmers/*"
                ).hasRole("UNION_REGISTRAR")

                .requestMatchers(
                    HttpMethod.PATCH, "/api/farmers/*/status"
                ).hasRole("UNION_REGISTRAR")

                // Training records: registrar writes, authorised staff read.
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/farmers/*/trainings"
                ).hasRole("UNION_REGISTRAR")

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/farmers/*/trainings",
                    "/api/farmers/*/missing-prerequisites"
                ).hasAnyRole(
                    "FARMER",
                    "UNION_REGISTRAR",
                    "FIELD_OFFICER",
                    "DISTRICT_AGRONOMIST"
                )

                // User administration: system administrator only.
                .requestMatchers("/api/users", "/api/users/**")
                    .hasRole("SYSTEM_ADMIN")

                // Password changes remain available to authenticated users.
                .requestMatchers(
                    HttpMethod.POST, "/api/auth/change-password"
                ).authenticated()

                // Audit logs: system administrator only.
                .requestMatchers("/api/audit-logs", "/api/audit-logs/**")
                    .hasRole("SYSTEM_ADMIN")

                // Current authenticated farmer profile endpoint.
                .requestMatchers(
                    HttpMethod.GET, "/api/me/profile"
                ).hasRole("FARMER")

                // Do not expose new endpoints by accident.
                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationEntryPoint(
                    errorHandlers.authenticationEntryPoint())
                .accessDeniedHandler(errorHandlers.accessDeniedHandler())
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
            );

        return http.build();
    }
}