package com.acms.service;

import com.acms.entity.AppUser;
import com.acms.entity.ProgrammeOffering;
import com.acms.enums.Role;
import org.springframework.security.access.AccessDeniedException;

/**
 * Who may act on one programme offering, in one place:
 *  - its Field Officer: assigned to the offering AND still in the offering's district
 *  - a District Agronomist: of the district that runs the programme
 *  - Union Registrar and System Admin: read everything
 * Every refusal is a 403 (the GlobalExceptionHandler also records it as ACCESS_DENIED).
 */
public final class OfferingAccess {

    private static final String NO_PERMISSION = "You do not have permission to perform this action.";

    private OfferingAccess() {
    }

    public static boolean isAssignedFieldOfficer(AppUser user, ProgrammeOffering offering) {
        return user.getRole() == Role.FIELD_OFFICER
                && sameDistrict(user, offering)
                && offering.getFieldOfficer().getId().equals(user.getId());
    }

    public static boolean isDistrictAgronomist(AppUser user, ProgrammeOffering offering) {
        return user.getRole() == Role.DISTRICT_AGRONOMIST && sameDistrict(user, offering);
    }

    public static void requireAssignedFieldOfficer(AppUser user, ProgrammeOffering offering) {
        if (!isAssignedFieldOfficer(user, offering)) {
            throw new AccessDeniedException(NO_PERMISSION);
        }
    }

    public static void requireDistrictAgronomist(AppUser user, ProgrammeOffering offering) {
        if (!isDistrictAgronomist(user, offering)) {
            throw new AccessDeniedException(NO_PERMISSION);
        }
    }

    /** Staff who may READ an offering's inspections and results. */
    public static void requireStaffView(AppUser user, ProgrammeOffering offering) {
        boolean allowed = user.getRole() == Role.UNION_REGISTRAR
                || user.getRole() == Role.SYSTEM_ADMIN
                || isDistrictAgronomist(user, offering)
                || isAssignedFieldOfficer(user, offering);
        if (!allowed) {
            throw new AccessDeniedException(NO_PERMISSION);
        }
    }

    private static boolean sameDistrict(AppUser user, ProgrammeOffering offering) {
        return user.getDistrict() != null
                && user.getDistrict().getId().equals(offering.getProgramme().getDistrict().getId());
    }
}
