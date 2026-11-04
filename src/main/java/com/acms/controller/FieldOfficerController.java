package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.config.ApiTags;
import com.acms.dto.FieldOfficerOptionResponse;
import com.acms.service.FieldOfficerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only lookup for the offering form: GET /api/field-officers?districtId={id}.
 * Requires OFFERING_MANAGE (Union Registrar), the same permission as creating an offering.
 * Not paged: a district has only a few Field Officers, and a dropdown needs them all.
 */
@Tag(name = ApiTags.OFFERINGS)
@RestController
@RequestMapping("/api/field-officers")
@RequiredArgsConstructor
public class FieldOfficerController extends BaseController {

    private final FieldOfficerService fieldOfficerService;

    @Operation(
            summary = "List Field Officers to assign",
            description = """
                    Enabled Field Officers to assign to an offering, by username; `?districtId=` (optional) limits to one district; id, username and district only.

                    - **Permission:** `OFFERING_MANAGE` (Registrar)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 400 districtId not a number, 404 unknown district
                    """)
    @GetMapping
    public ApiResponse<List<FieldOfficerOptionResponse>> list(@RequestParam(required = false) Long districtId) {
        return ok(fieldOfficerService.findAssignable(districtId));
    }
}