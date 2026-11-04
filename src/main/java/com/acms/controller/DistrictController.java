package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.config.ApiTags;
import com.acms.dto.DistrictRequest;
import com.acms.dto.DistrictResponse;
import com.acms.service.DistrictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** No POST: the 30 districts are seeded automatically. PUT only corrects a name (Registrar). */
@Tag(name = ApiTags.REFERENCE_DATA)
@RestController
@RequestMapping("/api/districts")
@RequiredArgsConstructor
public class DistrictController extends BaseController {

    private final DistrictService districtService;

    @Operation(
            summary = "List districts",
            description = """
                    The 30 districts (by province).

                    - **Permission:** `REFERENCE_READ` (all roles)
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, name, createdAt; default name), `direction` (asc or desc; default asc)
                    - **Success:** 200
                    """)
    @GetMapping
    public ApiResponse<PagedResponse<DistrictResponse>> list(@RequestParam(required = false) Long provinceId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "50") int size,
                                               @RequestParam(defaultValue = "name") String sortBy,
                                               @RequestParam(defaultValue = "asc") String direction) {
        return ok(districtService.findAll(provinceId, page, size, sortBy, direction));
    }

    @Operation(
            summary = "One district with its province",
            description = """
                    One district with its province.

                    - **Permission:** `REFERENCE_READ` (all roles)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/{id}")
    public ApiResponse<DistrictResponse> get(@PathVariable Long id) {
        return ok(districtService.findById(id));
    }

    @Operation(
            summary = "Correct a district name",
            description = """
                    Correct a district name.

                    - **Permission:** `REFERENCE_MANAGE` (Registrar)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 400, 404, 409 duplicate name
                    """)
    @PutMapping("/{id}")
    public ApiResponse<DistrictResponse> update(@PathVariable Long id, @Valid @RequestBody DistrictRequest request) {
        return ok(districtService.update(id, request));
    }
}
