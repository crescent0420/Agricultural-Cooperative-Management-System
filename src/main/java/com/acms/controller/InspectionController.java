package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.config.ApiTags;
import com.acms.dto.InspectionListResponse;
import com.acms.dto.InspectionRequest;
import com.acms.dto.InspectionResponse;
import com.acms.service.InspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = ApiTags.INSPECTIONS_AND_RESULTS)
@RestController
@RequiredArgsConstructor
public class InspectionController extends BaseController {

    private final InspectionService inspectionService;

    @Operation(
            summary = "Create an inspection",
            description = """
                    Create an inspection (max score, weight).

                    - **Permission:** `INSPECTION_MANAGE` (Field Officer)
                    - **Scope:** only the caller's own district, offerings or records; any other record gives 403
                    - **Success:** 201
                    - **Errors** (besides 401/403): 400, 404, 409 duplicate name / results already submitted, 422 weights above 100%
                    """)
    @PostMapping("/api/offerings/{offeringId}/inspections")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InspectionResponse> create(@PathVariable Long offeringId, @Valid @RequestBody InspectionRequest request) {
        return created(inspectionService.create(offeringId, request));
    }

    @Operation(
            summary = "Inspections of an offering",
            description = """
                    Inspections of an offering.

                    - **Permission:** `INSPECTION_READ` (Field Officer, Agronomist, Registrar, Admin)
                    - **Scope:** results are limited to the caller's own district, offerings or records
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/api/offerings/{offeringId}/inspections")
    public ApiResponse<InspectionListResponse> list(@PathVariable Long offeringId) {
        return ok(inspectionService.list(offeringId));
    }

    @Operation(
            summary = "Change an inspection",
            description = """
                    Change an inspection.

                    - **Permission:** `INSPECTION_MANAGE` (Field Officer)
                    - **Scope:** only the caller's own district, offerings or records; any other record gives 403
                    - **Success:** 200
                    - **Errors** (besides 401/403): 400, 404, 409 duplicate name / scores entered / results submitted, 422 weights above 100%
                    """)
    @PutMapping("/api/inspections/{id}")
    public ApiResponse<InspectionResponse> update(@PathVariable Long id, @Valid @RequestBody InspectionRequest request) {
        return ok(inspectionService.update(id, request));
    }

    @Operation(
            summary = "Delete an inspection without scores",
            description = """
                    Delete an inspection without scores.

                    - **Permission:** `INSPECTION_MANAGE` (Field Officer)
                    - **Scope:** only the caller's own district, offerings or records; any other record gives 403
                    - **Success:** 204
                    - **Errors** (besides 401/403): 404, 409 scores entered / results submitted
                    """)
    @DeleteMapping("/api/inspections/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        inspectionService.delete(id);
    }
}
