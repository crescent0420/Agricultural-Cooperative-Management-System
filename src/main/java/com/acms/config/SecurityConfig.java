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

    /** Every real role. A user who must still change the one-time password has none of them. */
    private static final String[] ALL_ROLES = {
        "FARMER", "FIELD_OFFICER", "DISTRICT_AGRONOMIST",
        "UNION_REGISTRAR", "FINANCE_OFFICER", "SYSTEM_ADMIN"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            AppUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DatabaseBackedJwtConverter jwtConverter,
            SecurityErrorHandlers errorHandlers) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(errorHandlers.authenticationEntryPoint())
                .accessDeniedHandler(errorHandlers.accessDeniedHandler()))
            .authorizeHttpRequests(auth -> auth

                // ---- Public ----
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/verify-email").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/error").permitAll()

                // ---- Any logged-in user, INCLUDING one who must still change the one-time password ----
                .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/auth/change-password").authenticated()

                // ---- Reference data: every real role may read, only union registrars may change ----
                .requestMatchers(HttpMethod.GET,
                    "/api/districts", "/api/districts/**",
                    "/api/cooperatives", "/api/cooperatives/**",
                    "/api/trainings", "/api/trainings/**",
                    "/api/programmes", "/api/programmes/**").hasAnyRole(ALL_ROLES)
                .requestMatchers(HttpMethod.POST,
                    "/api/districts", "/api/cooperatives", "/api/trainings",
                    "/api/programmes", "/api/programmes/*/prerequisites").hasRole("UNION_REGISTRAR")
                .requestMatchers(HttpMethod.PUT,
                    "/api/districts/**", "/api/cooperatives/**",
                    "/api/trainings/**", "/api/programmes/**").hasRole("UNION_REGISTRAR")
                .requestMatchers(HttpMethod.DELETE, "/api/programmes/*/prerequisites/*").hasRole("UNION_REGISTRAR")

                // ---- Seasons and enrolment windows: every role may read, only the Registrar may change ----
                .requestMatchers(HttpMethod.GET, "/api/seasons", "/api/seasons/**").hasAnyRole(ALL_ROLES)
                .requestMatchers(HttpMethod.POST,
                    "/api/seasons", "/api/seasons/*/windows/open", "/api/seasons/*/windows/close")
                    .hasRole("UNION_REGISTRAR")
                .requestMatchers(HttpMethod.PUT, "/api/seasons/*").hasRole("UNION_REGISTRAR")

                // ---- Farmers ----
                .requestMatchers(HttpMethod.GET, "/api/farmers").hasAnyRole(
                    "UNION_REGISTRAR", "FIELD_OFFICER", "DISTRICT_AGRONOMIST", "FINANCE_OFFICER", "SYSTEM_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/farmers/*").hasAnyRole(
                    "FARMER", "UNION_REGISTRAR", "FIELD_OFFICER", "DISTRICT_AGRONOMIST")
                .requestMatchers(HttpMethod.POST, "/api/farmers").hasRole("UNION_REGISTRAR")
                .requestMatchers(HttpMethod.PUT, "/api/farmers/*").hasRole("UNION_REGISTRAR")
                .requestMatchers(HttpMethod.PATCH, "/api/farmers/*/status").hasRole("UNION_REGISTRAR")

                // ---- Training records ----
                .requestMatchers(HttpMethod.POST, "/api/farmers/*/trainings").hasRole("UNION_REGISTRAR")
                .requestMatchers(HttpMethod.GET,
                    "/api/farmers/*/trainings", "/api/farmers/*/missing-prerequisites").hasAnyRole(
                    "FARMER", "UNION_REGISTRAR", "FIELD_OFFICER", "DISTRICT_AGRONOMIST")

                // ---- Administration ----
                .requestMatchers("/api/users", "/api/users/**").hasRole("SYSTEM_ADMIN")
                .requestMatchers("/api/audit-logs", "/api/audit-logs/**").hasRole("SYSTEM_ADMIN")

                // ---- A farmer's own profile ----
                .requestMatchers(HttpMethod.GET, "/api/me/profile").hasRole("FARMER")

                // ---- Do not expose new endpoints by accident ----
                .anyRequest().denyAll())
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationEntryPoint(errorHandlers.authenticationEntryPoint())
                .accessDeniedHandler(errorHandlers.accessDeniedHandler())
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)));

        return http.build();
    }
}
