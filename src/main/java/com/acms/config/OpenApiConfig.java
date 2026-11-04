package com.acms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: /swagger-ui.html, OpenAPI description: /v3/api-docs.
 * The groups (tags) are listed here in display order with their descriptions;
 * each controller names its group with @Tag(name = ApiTags.X), and each endpoint has an @Operation.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI acmsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Agricultural Cooperative Management System API")
                        .version("1.0")
                        .description("""
                                REST API of the Rwanda Cooperative Union's ACMS: farmer registration, seasonal \
                                crop programme enrolment, field inspections and quality grades, input contributions \
                                and payments, collection permits and management reports.

                                **How to call it:** log in with `POST /api/auth/login`, then send the token as \
                                `Authorization: Bearer <accessToken>` (the **Authorize** button above). Tokens last \
                                30 minutes. Every answer uses the envelope `{success, message, data | error, timestamp}`.

                                **Access:** each endpoint needs one permission (listed in its description; the full \
                                matrix is `GET /api/permissions`). Field Officers and District Agronomists are also \
                                limited to their own district, and farmers to their own records; another record \
                                gives 403."""))
                .tags(List.of(
                        tag(ApiTags.AUTHENTICATION,
                                "Log in to get an access token, register, verify an email address, read the current "
                                        + "user and change the password."),
                        tag(ApiTags.USER_ADMINISTRATION,
                                "System Administrator only: create accounts (a one-time password is emailed), change "
                                        + "roles, enable or disable accounts and reset passwords."),
                        tag(ApiTags.AUDIT_AND_PERMISSIONS,
                                "The security audit log (System Administrator) and the role-permission matrix "
                                        + "(every role)."),
                        tag(ApiTags.REFERENCE_DATA,
                                "Provinces and districts of Rwanda, cooperatives, crop programmes with their "
                                        + "prerequisite trainings, and trainings. Everyone reads; the Union Registrar manages."),
                        tag(ApiTags.FARMERS,
                                "Farmer registration with a generated membership number, search with paging and "
                                        + "sorting, status changes, and a farmer's own profile."),
                        tag(ApiTags.TRAINING_RECORDS,
                                "Completed trainings of a farmer, and the prerequisite trainings still missing for a "
                                        + "crop programme."),
                        tag(ApiTags.SEASONS,
                                "Seasons (dates, land limit, distribution day) and the enrolment windows the Union "
                                        + "Registrar opens and closes."),
                        tag(ApiTags.OFFERINGS,
                                "A crop programme offered in one season, with a Field Officer, land per participant "
                                        + "and a maximum number of places; and the Field Officer lookup used to assign one."),
                        tag(ApiTags.ENROLMENT,
                                "Farmers enrol in offerings (every rule checked in one transaction; capacity is never "
                                        + "exceeded), and enrolments are listed and cancelled."),
                        tag(ApiTags.CONTRIBUTIONS_AND_PAYMENTS,
                                "What each farmer owes per season, adjustments with history, and payments that count "
                                        + "only once confirmed; a repeated request never creates a second payment."),
                        tag(ApiTags.INSPECTIONS_AND_RESULTS,
                                "Inspections with maximum scores and weights, score entry, Draft, Submitted and "
                                        + "Published results with the configurable grading scale, and authorised "
                                        + "corrections with history."),
                        tag(ApiTags.COLLECTION_PERMITS,
                                "Permits to collect inputs, issued only when fully paid, Active and before the "
                                        + "distribution day; verification by permit number and the record of every attempt."),
                        tag(ApiTags.MANAGEMENT_REPORTS,
                                "Farmers per cooperative, offering capacity, outstanding contributions and grade "
                                        + "distribution; district staff see only their own district.")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    private static Tag tag(String name, String description) {
        return new Tag().name(name).description(description);
    }
}
