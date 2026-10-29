package com.acms.security;

import static com.acms.entity.Role.DISTRICT_AGRONOMIST;
import static com.acms.entity.Role.FARMER;
import static com.acms.entity.Role.FIELD_OFFICER;
import static com.acms.entity.Role.FINANCE_OFFICER;
import static com.acms.entity.Role.SYSTEM_ADMIN;
import static com.acms.entity.Role.UNION_REGISTRAR;
import static com.acms.security.Permission.*;

import com.acms.entity.Role;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * THE permission matrix: which role may do what, in one place.
 *   - DatabaseBackedJwtConverter gives a logged-in user the permissions of their role.
 *   - SecurityConfig requires one permission per URL.
 *   - GET /api/permissions shows this table, and docs/permission-matrix.md documents it.
 * Changing what a role may do means changing ONE line here.
 *
 * This decides access to a FUNCTION. Access to a particular RECORD (my own farmer profile, my district's
 * offerings) is checked afterwards in the services (FarmerAccessService, OfferingAccess...).
 */
public final class PermissionMatrix {

    private static final Map<Permission, Set<Role>> ROLES_BY_PERMISSION = new EnumMap<>(Permission.class);

    static {
        grant(REFERENCE_READ, FARMER, FIELD_OFFICER, DISTRICT_AGRONOMIST, UNION_REGISTRAR, FINANCE_OFFICER, SYSTEM_ADMIN);
        grant(REFERENCE_MANAGE, UNION_REGISTRAR);
        grant(SEASON_READ, FARMER, FIELD_OFFICER, DISTRICT_AGRONOMIST, UNION_REGISTRAR, FINANCE_OFFICER, SYSTEM_ADMIN);
        grant(SEASON_MANAGE, UNION_REGISTRAR);
        grant(OFFERING_READ, FARMER, FIELD_OFFICER, DISTRICT_AGRONOMIST, UNION_REGISTRAR, SYSTEM_ADMIN);
        grant(OFFERING_READ_ASSIGNED, FIELD_OFFICER);
        grant(OFFERING_MANAGE, UNION_REGISTRAR);
        grant(ENROLMENT_CREATE, FARMER);
        grant(ENROLMENT_READ_OWN, FARMER);
        grant(ENROLMENT_READ, FARMER, FIELD_OFFICER, DISTRICT_AGRONOMIST, UNION_REGISTRAR, SYSTEM_ADMIN);
        grant(ENROLMENT_LIST_FOR_OFFERING, FIELD_OFFICER, DISTRICT_AGRONOMIST, UNION_REGISTRAR, SYSTEM_ADMIN);
        grant(ENROLMENT_CANCEL, FARMER, UNION_REGISTRAR);
        grant(CONTRIBUTION_READ_OWN, FARMER);
        grant(CONTRIBUTION_READ, FARMER, UNION_REGISTRAR, FINANCE_OFFICER);
        grant(CONTRIBUTION_LIST, UNION_REGISTRAR, FINANCE_OFFICER);
        grant(CONTRIBUTION_MANAGE, FINANCE_OFFICER);
        grant(PAYMENT_MANAGE, FINANCE_OFFICER);
        grant(FARMER_LIST, FIELD_OFFICER, DISTRICT_AGRONOMIST, UNION_REGISTRAR, FINANCE_OFFICER, SYSTEM_ADMIN);
        grant(FARMER_READ, FARMER, FIELD_OFFICER, DISTRICT_AGRONOMIST, UNION_REGISTRAR);
        grant(FARMER_MANAGE, UNION_REGISTRAR);
        grant(FARMER_STATUS_CHANGE, UNION_REGISTRAR);
        grant(PROFILE_READ_OWN, FARMER);
        grant(TRAINING_RECORD_READ, FARMER, FIELD_OFFICER, DISTRICT_AGRONOMIST, UNION_REGISTRAR);
        grant(TRAINING_RECORD_MANAGE, UNION_REGISTRAR);
        grant(GRADING_SCALE_READ, FARMER, FIELD_OFFICER, DISTRICT_AGRONOMIST, UNION_REGISTRAR, FINANCE_OFFICER, SYSTEM_ADMIN);
        grant(GRADING_SCALE_MANAGE, UNION_REGISTRAR);
        grant(INSPECTION_READ, FIELD_OFFICER, DISTRICT_AGRONOMIST, UNION_REGISTRAR, SYSTEM_ADMIN);
        grant(INSPECTION_MANAGE, FIELD_OFFICER);
        grant(RESULT_ENTER, FIELD_OFFICER);
        grant(RESULT_APPROVE, DISTRICT_AGRONOMIST);
        grant(RESULT_CORRECT, DISTRICT_AGRONOMIST);
        grant(RESULT_READ, FARMER, FIELD_OFFICER, DISTRICT_AGRONOMIST, UNION_REGISTRAR, SYSTEM_ADMIN);
        grant(RESULT_READ_OWN, FARMER);
        grant(PERMIT_GENERATE, FARMER);
        grant(PERMIT_READ_OWN, FARMER);
        grant(PERMIT_VERIFY, FIELD_OFFICER, DISTRICT_AGRONOMIST, UNION_REGISTRAR, FINANCE_OFFICER, SYSTEM_ADMIN);
        grant(PERMIT_ATTEMPT_READ, UNION_REGISTRAR, SYSTEM_ADMIN);
        grant(USER_MANAGE, SYSTEM_ADMIN);
        grant(AUDIT_LOG_READ, SYSTEM_ADMIN);
        grant(PERMISSION_MATRIX_READ, FARMER, FIELD_OFFICER, DISTRICT_AGRONOMIST, UNION_REGISTRAR, FINANCE_OFFICER, SYSTEM_ADMIN);

        // Every permission must be granted to somebody (catches a forgotten line at start-up).
        for (Permission permission : Permission.values()) {
            if (!ROLES_BY_PERMISSION.containsKey(permission)) {
                throw new IllegalStateException("Permission " + permission + " is not granted to any role.");
            }
        }
    }

    private PermissionMatrix() {
    }

    private static void grant(Permission permission, Role... roles) {
        ROLES_BY_PERMISSION.put(permission, Collections.unmodifiableSet(EnumSet.of(roles[0], roles)));
    }

    /** The roles that hold this permission. */
    public static Set<Role> rolesWith(Permission permission) {
        return ROLES_BY_PERMISSION.get(permission);
    }

    /** Everything this role may do, in the order of the Permission enum. */
    public static List<Permission> permissionsOf(Role role) {
        return ROLES_BY_PERMISSION.entrySet().stream()
                .filter(entry -> entry.getValue().contains(role))
                .map(Map.Entry::getKey)
                .toList();
    }

    public static boolean has(Role role, Permission permission) {
        return rolesWith(permission).contains(role);
    }
}
