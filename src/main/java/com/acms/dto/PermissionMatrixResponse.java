package com.acms.dto;

import com.acms.entity.Role;
import com.acms.security.Permission;
import com.acms.security.PermissionMatrix;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /api/permissions: the matrix two ways.
 *   rows   - one line per permission: which roles hold it (the table in docs/permission-matrix.md)
 *   byRole - per role: everything it may do
 */
public record PermissionMatrixResponse(List<Row> rows, Map<Role, List<Permission>> byRole) {

    public record Row(Permission permission, String group, String description, List<Role> roles) {
    }

    public static PermissionMatrixResponse current() {
        List<Row> rows = Arrays.stream(Permission.values())
                .map(p -> new Row(p, p.group(), p.description(),
                        Arrays.stream(Role.values()).filter(r -> PermissionMatrix.has(r, p)).toList()))
                .toList();
        Map<Role, List<Permission>> byRole = new LinkedHashMap<>();
        for (Role role : Role.values()) {
            byRole.put(role, PermissionMatrix.permissionsOf(role));
        }
        return new PermissionMatrixResponse(rows, byRole);
    }
}
