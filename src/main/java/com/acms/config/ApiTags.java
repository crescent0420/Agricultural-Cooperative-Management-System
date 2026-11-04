package com.acms.config;

/**
 * The 13 groups shown in Swagger UI, in the order a user meets the system.
 * Controllers use these constants in @Tag, so a typo cannot create a 14th group;
 * OpenApiConfig gives each group its description and its place in the list,
 * and OpenApiDocumentationTest checks that every endpoint uses one of them.
 */
public final class ApiTags {

    public static final String AUTHENTICATION = "Authentication";
    public static final String USER_ADMINISTRATION = "User administration";
    public static final String AUDIT_AND_PERMISSIONS = "Audit and permissions";
    public static final String REFERENCE_DATA = "Reference data";
    public static final String FARMERS = "Farmers";
    public static final String TRAINING_RECORDS = "Training records";
    public static final String SEASONS = "Seasons and enrolment windows";
    public static final String OFFERINGS = "Programme offerings";
    public static final String ENROLMENT = "Enrolment";
    public static final String CONTRIBUTIONS_AND_PAYMENTS = "Contributions and payments";
    public static final String INSPECTIONS_AND_RESULTS = "Inspections and results";
    public static final String COLLECTION_PERMITS = "Collection permits";
    public static final String MANAGEMENT_REPORTS = "Management reports";

    private ApiTags() {
    }
}
