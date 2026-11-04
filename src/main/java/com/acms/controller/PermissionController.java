package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.config.ApiTags;
import com.acms.dto.PermissionMatrixResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The permission matrix, live from PermissionMatrix (the same table the security rules use). */
@Tag(name = ApiTags.AUDIT_AND_PERMISSIONS)
@RestController
@RequestMapping("/api/permissions")
public class PermissionController extends BaseController {

    @Operation(
            summary = "The permission matrix",
            description = """
                    The permission matrix (role → permissions).

                    - **Permission:** `PERMISSION_MATRIX_READ` (all roles)
                    - **Success:** 200
                    """)
    @GetMapping
    public ApiResponse<PermissionMatrixResponse> matrix() {
        return ok(PermissionMatrixResponse.current());
    }
}
