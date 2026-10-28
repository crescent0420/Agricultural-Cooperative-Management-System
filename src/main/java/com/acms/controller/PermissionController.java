package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.dto.PermissionMatrixResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The permission matrix, live from PermissionMatrix (the same table the security rules use). */
@RestController
@RequestMapping("/api/permissions")
public class PermissionController extends BaseController {

    @GetMapping
    public ApiResponse<PermissionMatrixResponse> matrix() {
        return ok(PermissionMatrixResponse.current());
    }
}
