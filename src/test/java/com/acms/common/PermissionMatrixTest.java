package com.acms.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.acms.entity.Role;
import com.acms.security.Permission;
import com.acms.security.PermissionMatrix;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Plain unit test (no Spring, no database): the matrix encodes the separation of duties in the brief. */
class PermissionMatrixTest {

    @Test
    @DisplayName("Every permission is granted to at least one role")
    void everyPermissionHasARole() {
        for (Permission permission : Permission.values()) {
            assertThat(PermissionMatrix.rolesWith(permission)).as(permission.name()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("Separation of duties: money for Finance only, field data never for Finance, accounts for the Admin")
    void separationOfDuties() {
        assertThat(PermissionMatrix.rolesWith(Permission.PAYMENT_MANAGE)).containsExactly(Role.FINANCE_OFFICER);
        assertThat(PermissionMatrix.rolesWith(Permission.CONTRIBUTION_MANAGE)).containsExactly(Role.FINANCE_OFFICER);
        assertThat(PermissionMatrix.permissionsOf(Role.FINANCE_OFFICER))
                .doesNotContain(Permission.FARMER_MANAGE, Permission.FARMER_STATUS_CHANGE,
                        Permission.OFFERING_MANAGE, Permission.TRAINING_RECORD_MANAGE);
        assertThat(PermissionMatrix.rolesWith(Permission.FARMER_STATUS_CHANGE)).containsExactly(Role.UNION_REGISTRAR);
        assertThat(PermissionMatrix.rolesWith(Permission.USER_MANAGE)).containsExactly(Role.SYSTEM_ADMIN);
        assertThat(PermissionMatrix.rolesWith(Permission.ENROLMENT_CREATE)).containsExactly(Role.FARMER);
    }

    @Test
    @DisplayName("A farmer can read their own data but cannot list farmers or touch payments")
    void farmerPermissions() {
        assertThat(PermissionMatrix.permissionsOf(Role.FARMER))
                .contains(Permission.PROFILE_READ_OWN, Permission.ENROLMENT_READ_OWN, Permission.CONTRIBUTION_READ_OWN)
                .doesNotContain(Permission.FARMER_LIST, Permission.PAYMENT_MANAGE, Permission.USER_MANAGE);
    }
}
