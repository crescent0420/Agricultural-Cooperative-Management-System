package com.acms.security;

/**
 * Everything a user can be allowed to do. Each permission is granted to roles in PermissionMatrix,
 * and every URL in SecurityConfig requires exactly one permission.
 * The enum name is also the Spring Security authority (hasAuthority("FARMER_LIST")).
 */
public enum Permission {

    // ---- Reference data ----
    REFERENCE_READ("Reference data", "Read provinces, districts, cooperatives, trainings and crop programmes"),
    REFERENCE_MANAGE("Reference data", "Create and change cooperatives, trainings, programmes and prerequisites; edit districts"),

    // ---- Seasons ----
    SEASON_READ("Seasons", "Read seasons and enrolment windows"),
    SEASON_MANAGE("Seasons", "Create and change seasons; open and close enrolment windows"),

    // ---- Programme offerings ----
    OFFERING_READ("Programme offerings", "Read programme offerings (own district only for farmers, officers and agronomists)"),
    OFFERING_READ_ASSIGNED("Programme offerings", "Read the offerings assigned to me"),
    OFFERING_MANAGE("Programme offerings", "Create and change programme offerings"),

    // ---- Enrolment ----
    ENROLMENT_CREATE("Enrolment", "Enrol myself in an offering"),
    ENROLMENT_READ_OWN("Enrolment", "Read my own enrolments"),
    ENROLMENT_READ("Enrolment", "Read one enrolment (own, or within my offerings or district)"),
    ENROLMENT_LIST_FOR_OFFERING("Enrolment", "List the farmers enrolled in an offering (own offering or district)"),
    ENROLMENT_CANCEL("Enrolment", "Cancel an enrolment (farmers: their own, while the window is open)"),

    // ---- Contributions and payments ----
    CONTRIBUTION_READ_OWN("Contributions and payments", "Read my own contributions and balances"),
    CONTRIBUTION_READ("Contributions and payments", "Read one contribution, its payments, or one payment (farmers: their own)"),
    CONTRIBUTION_LIST("Contributions and payments", "List all contributions and their adjustment history"),
    CONTRIBUTION_MANAGE("Contributions and payments", "Record contributions and adjust the amount owed"),
    PAYMENT_MANAGE("Contributions and payments", "Record, confirm and fail payments"),

    // ---- Farmers ----
    FARMER_LIST("Farmers", "Search farmers (own district only for officers and agronomists)"),
    FARMER_READ("Farmers", "Read one farmer (farmers: only themselves)"),
    FARMER_MANAGE("Farmers", "Register farmers and change their details"),
    FARMER_STATUS_CHANGE("Farmers", "Change a farmer's status (Active, Suspended, Withdrawn, Retired)"),
    PROFILE_READ_OWN("Farmers", "Read my own farmer profile"),

    // ---- Training records ----
    TRAINING_RECORD_READ("Training records", "Read a farmer's completed trainings and missing prerequisites"),
    TRAINING_RECORD_MANAGE("Training records", "Record a completed training for a farmer"),

    // ---- Inspections and results ----
    GRADING_SCALE_READ("Inspections and results", "Read the grading scale"),
    GRADING_SCALE_MANAGE("Inspections and results", "Replace the grading scale"),
    INSPECTION_READ("Inspections and results", "Read an offering's inspections and results (own offering or district)"),
    INSPECTION_MANAGE("Inspections and results", "Define the inspections (max score, weight) of my own offering"),
    RESULT_ENTER("Inspections and results", "Enter scores and submit results for my own offering"),
    RESULT_APPROVE("Inspections and results", "Approve (publish) or return submitted results of my district"),
    RESULT_CORRECT("Inspections and results", "Correct a published result of my district, with a reason"),
    RESULT_READ("Inspections and results", "Read one result and its corrections (farmers: their own published results only)"),
    RESULT_READ_OWN("Inspections and results", "Read my own published results"),

    // ---- Administration ----
    USER_MANAGE("Administration", "Create accounts, change roles, enable/disable, reset passwords"),
    AUDIT_LOG_READ("Administration", "Read the audit log"),
    PERMISSION_MATRIX_READ("Administration", "Read this permission matrix");

    private final String group;
    private final String description;

    Permission(String group, String description) {
        this.group = group;
        this.description = description;
    }

    public String group() {
        return group;
    }

    public String description() {
        return description;
    }

    /** The authority string checked by Spring Security. */
    public String authority() {
        return name();
    }
}
