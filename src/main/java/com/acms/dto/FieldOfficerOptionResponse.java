package com.acms.dto;

import com.acms.entity.AppUser;

/**
 * GET /api/field-officers: one choice in the Registrar's "Field Officer" dropdown.
 * Only what the dropdown needs: no email, no account flags (least privilege).
 */
public record FieldOfficerOptionResponse(
        Long id,
        String username,
        Long districtId,
        String districtName) {

    /** A Field Officer always has a district (database check chk_app_users_role_scope). */
    public static FieldOfficerOptionResponse from(AppUser officer) {
        return new FieldOfficerOptionResponse(
                officer.getId(),
                officer.getUsername(),
                officer.getDistrict().getId(),
                officer.getDistrict().getName());
    }
}