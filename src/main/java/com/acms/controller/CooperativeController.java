package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.config.ApiTags;
import com.acms.dto.CooperativeRenameRequest;
import com.acms.dto.CooperativeRequest;
import com.acms.dto.CooperativeResponse;
import com.acms.service.CooperativeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = ApiTags.REFERENCE_DATA)
@RestController
@RequestMapping("/api/cooperatives")
public class CooperativeController extends BaseController {

    private final CooperativeService cooperativeService;

    public CooperativeController(CooperativeService cooperativeService) {
        this.cooperativeService = cooperativeService;
    }

    @Operation(
            summary = "Create a cooperative in a district",
            description = """
                    Create a cooperative in a district.

                    - **Permission:** `REFERENCE_MANAGE` (Registrar)
                    - **Success:** 201
                    - **Errors** (besides 401/403): 400, 409 duplicate name in the district
                    """)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CooperativeResponse> create(
            @Valid @RequestBody CooperativeRequest request) {
        return created(cooperativeService.create(request));
    }

    @Operation(
            summary = "List cooperatives",
            description = """
                    List cooperatives.

                    - **Permission:** `REFERENCE_READ` (all roles)
                    - **Scope:** results are limited to the caller's own district, offerings or records
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, name, createdAt; default name), `direction` (asc or desc; default asc)
                    - **Success:** 200
                    """)
    @GetMapping
    public ApiResponse<PagedResponse<CooperativeResponse>> list(
            @RequestParam(required = false) Long districtId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return ok(cooperativeService.findAll(
                districtId, page, size, sortBy, direction));
    }

    @Operation(
            summary = "One cooperative",
            description = """
                    One cooperative.

                    - **Permission:** `REFERENCE_READ` (all roles)
                    - **Scope:** only the caller's own district, offerings or records; any other record gives 403
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/{id}")
    public ApiResponse<CooperativeResponse> get(@PathVariable Long id) {
        return ok(cooperativeService.findById(id));
    }

    @Operation(
            summary = "Rename a cooperative",
            description = """
                    Rename a cooperative.

                    - **Permission:** `REFERENCE_MANAGE` (Registrar)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 400, 404, 409 duplicate name
                    """)
    @PutMapping("/{id}")
    public ApiResponse<CooperativeResponse> rename(
            @PathVariable Long id,
            @Valid @RequestBody CooperativeRenameRequest request) {
        return ok(cooperativeService.rename(id, request));
    }
}
