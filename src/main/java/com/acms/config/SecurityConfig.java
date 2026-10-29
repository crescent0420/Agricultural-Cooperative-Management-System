package com.acms.config;

import static com.acms.security.Permission.AUDIT_LOG_READ;
import static com.acms.security.Permission.CONTRIBUTION_LIST;
import static com.acms.security.Permission.CONTRIBUTION_MANAGE;
import static com.acms.security.Permission.CONTRIBUTION_READ;
import static com.acms.security.Permission.CONTRIBUTION_READ_OWN;
import static com.acms.security.Permission.ENROLMENT_CANCEL;
import static com.acms.security.Permission.ENROLMENT_CREATE;
import static com.acms.security.Permission.ENROLMENT_LIST_FOR_OFFERING;
import static com.acms.security.Permission.ENROLMENT_READ;
import static com.acms.security.Permission.ENROLMENT_READ_OWN;
import static com.acms.security.Permission.FARMER_LIST;
import static com.acms.security.Permission.FARMER_MANAGE;
import static com.acms.security.Permission.FARMER_READ;
import static com.acms.security.Permission.FARMER_STATUS_CHANGE;
import static com.acms.security.Permission.GRADING_SCALE_MANAGE;
import static com.acms.security.Permission.GRADING_SCALE_READ;
import static com.acms.security.Permission.INSPECTION_MANAGE;
import static com.acms.security.Permission.INSPECTION_READ;
import static com.acms.security.Permission.OFFERING_MANAGE;
import static com.acms.security.Permission.OFFERING_READ;
import static com.acms.security.Permission.OFFERING_READ_ASSIGNED;
import static com.acms.security.Permission.PAYMENT_MANAGE;
import static com.acms.security.Permission.PERMISSION_MATRIX_READ;
import static com.acms.security.Permission.PERMIT_ATTEMPT_READ;
import static com.acms.security.Permission.PERMIT_GENERATE;
import static com.acms.security.Permission.PERMIT_READ_OWN;
import static com.acms.security.Permission.PERMIT_VERIFY;
import static com.acms.security.Permission.PROFILE_READ_OWN;
import static com.acms.security.Permission.REFERENCE_MANAGE;
import static com.acms.security.Permission.REFERENCE_READ;
import static com.acms.security.Permission.RESULT_APPROVE;
import static com.acms.security.Permission.RESULT_CORRECT;
import static com.acms.security.Permission.RESULT_ENTER;
import static com.acms.security.Permission.RESULT_READ;
import static com.acms.security.Permission.RESULT_READ_OWN;
import static com.acms.security.Permission.SEASON_MANAGE;
import static com.acms.security.Permission.SEASON_READ;
import static com.acms.security.Permission.TRAINING_RECORD_MANAGE;
import static com.acms.security.Permission.TRAINING_RECORD_READ;
import static com.acms.security.Permission.USER_MANAGE;

import com.acms.security.DatabaseBackedJwtConverter;
import com.acms.security.Permission;
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

/**
 * Access to FUNCTIONS: every URL needs one permission from the PermissionMatrix (which roles hold
 * which permission is decided there, not here). Access to RECORDS (own farmer, own district, own
 * offering) is checked afterwards in the services.
 */
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
            AppUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    /** The authority string of a permission, e.g. can(FARMER_LIST) = "FARMER_LIST". */
    private static String can(Permission permission) {
        return permission.authority();
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

                // ---- The permission matrix itself ----
                .requestMatchers(HttpMethod.GET, "/api/permissions").hasAuthority(can(PERMISSION_MATRIX_READ))

                // ---- Reference data (provinces and districts are seeded: no POST /api/districts -> denyAll) ----
                .requestMatchers(HttpMethod.GET,
                    "/api/provinces", "/api/provinces/**",
                    "/api/districts", "/api/districts/**",
                    "/api/cooperatives", "/api/cooperatives/**",
                    "/api/trainings", "/api/trainings/**",
                    "/api/programmes", "/api/programmes/**").hasAuthority(can(REFERENCE_READ))
                .requestMatchers(HttpMethod.POST,
                    "/api/cooperatives", "/api/trainings",
                    "/api/programmes", "/api/programmes/*/prerequisites").hasAuthority(can(REFERENCE_MANAGE))
                .requestMatchers(HttpMethod.PUT,
                    "/api/districts/**", "/api/cooperatives/**",
                    "/api/trainings/**", "/api/programmes/**").hasAuthority(can(REFERENCE_MANAGE))
                .requestMatchers(HttpMethod.DELETE, "/api/programmes/*/prerequisites/*").hasAuthority(can(REFERENCE_MANAGE))

                // ---- Seasons and enrolment windows ----
                .requestMatchers(HttpMethod.GET, "/api/seasons", "/api/seasons/**").hasAuthority(can(SEASON_READ))
                .requestMatchers(HttpMethod.POST,
                    "/api/seasons", "/api/seasons/*/windows/open", "/api/seasons/*/windows/close")
                    .hasAuthority(can(SEASON_MANAGE))
                .requestMatchers(HttpMethod.PUT, "/api/seasons/*").hasAuthority(can(SEASON_MANAGE))

                // ---- Programme offerings (the district filter is applied in ProgrammeOfferingService) ----
                .requestMatchers(HttpMethod.GET, "/api/offerings/assigned-to-me").hasAuthority(can(OFFERING_READ_ASSIGNED))
                .requestMatchers(HttpMethod.GET, "/api/offerings", "/api/offerings/*").hasAuthority(can(OFFERING_READ))
                .requestMatchers(HttpMethod.POST, "/api/offerings").hasAuthority(can(OFFERING_MANAGE))
                .requestMatchers(HttpMethod.PUT, "/api/offerings/*").hasAuthority(can(OFFERING_MANAGE))

                // ---- Enrolment (ownership and district are checked in EnrolmentService) ----
                .requestMatchers(HttpMethod.POST, "/api/offerings/*/enrolments").hasAuthority(can(ENROLMENT_CREATE))
                .requestMatchers(HttpMethod.GET, "/api/offerings/*/enrolments").hasAuthority(can(ENROLMENT_LIST_FOR_OFFERING))
                .requestMatchers(HttpMethod.GET, "/api/me/enrolments").hasAuthority(can(ENROLMENT_READ_OWN))
                .requestMatchers(HttpMethod.GET, "/api/enrolments/*").hasAuthority(can(ENROLMENT_READ))
                .requestMatchers(HttpMethod.POST, "/api/enrolments/*/cancel").hasAuthority(can(ENROLMENT_CANCEL))

                // ---- Contributions and payments (a farmer's own records are checked in the services) ----
                .requestMatchers(HttpMethod.GET, "/api/me/contributions").hasAuthority(can(CONTRIBUTION_READ_OWN))
                .requestMatchers(HttpMethod.POST, "/api/contributions", "/api/contributions/*/adjustments")
                    .hasAuthority(can(CONTRIBUTION_MANAGE))
                .requestMatchers(HttpMethod.GET, "/api/contributions", "/api/contributions/*/adjustments")
                    .hasAuthority(can(CONTRIBUTION_LIST))
                .requestMatchers(HttpMethod.GET, "/api/contributions/*", "/api/contributions/*/payments",
                    "/api/payments/*").hasAuthority(can(CONTRIBUTION_READ))
                .requestMatchers(HttpMethod.POST, "/api/payments", "/api/payments/*/confirm", "/api/payments/*/fail")
                    .hasAuthority(can(PAYMENT_MANAGE))

                // ---- Inspections and results (offering, district and stage rules are in the services) ----
                .requestMatchers(HttpMethod.GET, "/api/grading-scale").hasAuthority(can(GRADING_SCALE_READ))
                .requestMatchers(HttpMethod.PUT, "/api/grading-scale").hasAuthority(can(GRADING_SCALE_MANAGE))
                .requestMatchers(HttpMethod.GET, "/api/offerings/*/inspections", "/api/offerings/*/results")
                    .hasAuthority(can(INSPECTION_READ))
                .requestMatchers(HttpMethod.POST, "/api/offerings/*/inspections").hasAuthority(can(INSPECTION_MANAGE))
                .requestMatchers(HttpMethod.PUT, "/api/inspections/*").hasAuthority(can(INSPECTION_MANAGE))
                .requestMatchers(HttpMethod.DELETE, "/api/inspections/*").hasAuthority(can(INSPECTION_MANAGE))
                .requestMatchers(HttpMethod.PUT, "/api/enrolments/*/scores").hasAuthority(can(RESULT_ENTER))
                .requestMatchers(HttpMethod.POST, "/api/results/*/submit").hasAuthority(can(RESULT_ENTER))
                .requestMatchers(HttpMethod.POST, "/api/results/*/approve", "/api/results/*/return")
                    .hasAuthority(can(RESULT_APPROVE))
                .requestMatchers(HttpMethod.POST, "/api/results/*/corrections").hasAuthority(can(RESULT_CORRECT))
                .requestMatchers(HttpMethod.GET, "/api/me/results").hasAuthority(can(RESULT_READ_OWN))
                .requestMatchers(HttpMethod.GET, "/api/results/*", "/api/results/*/corrections",
                    "/api/farmers/*/results").hasAuthority(can(RESULT_READ))

                // ---- Collection permits (ownership and the 100% rule are checked in PermitService) ----
                .requestMatchers(HttpMethod.POST, "/api/enrolments/*/permit").hasAuthority(can(PERMIT_GENERATE))
                .requestMatchers(HttpMethod.GET, "/api/me/permits").hasAuthority(can(PERMIT_READ_OWN))
                .requestMatchers(HttpMethod.GET, "/api/permits/verify/*").hasAuthority(can(PERMIT_VERIFY))
                .requestMatchers(HttpMethod.GET, "/api/permits/attempts").hasAuthority(can(PERMIT_ATTEMPT_READ))

                // ---- Farmers ----
                .requestMatchers(HttpMethod.GET, "/api/farmers").hasAuthority(can(FARMER_LIST))
                .requestMatchers(HttpMethod.GET, "/api/farmers/*").hasAuthority(can(FARMER_READ))
                .requestMatchers(HttpMethod.POST, "/api/farmers").hasAuthority(can(FARMER_MANAGE))
                .requestMatchers(HttpMethod.PUT, "/api/farmers/*").hasAuthority(can(FARMER_MANAGE))
                .requestMatchers(HttpMethod.PATCH, "/api/farmers/*/status").hasAuthority(can(FARMER_STATUS_CHANGE))
                .requestMatchers(HttpMethod.GET, "/api/me/profile").hasAuthority(can(PROFILE_READ_OWN))

                // ---- Training records ----
                .requestMatchers(HttpMethod.POST, "/api/farmers/*/trainings").hasAuthority(can(TRAINING_RECORD_MANAGE))
                .requestMatchers(HttpMethod.GET,
                    "/api/farmers/*/trainings", "/api/farmers/*/missing-prerequisites")
                    .hasAuthority(can(TRAINING_RECORD_READ))

                // ---- Administration ----
                .requestMatchers("/api/users", "/api/users/**").hasAuthority(can(USER_MANAGE))
                .requestMatchers("/api/audit-logs", "/api/audit-logs/**").hasAuthority(can(AUDIT_LOG_READ))

                // ---- Do not expose new endpoints by accident ----
                .anyRequest().denyAll())
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationEntryPoint(errorHandlers.authenticationEntryPoint())
                .accessDeniedHandler(errorHandlers.accessDeniedHandler())
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)));

        return http.build();
    }
}
