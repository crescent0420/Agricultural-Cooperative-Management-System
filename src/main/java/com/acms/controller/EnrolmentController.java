package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.config.ApiTags;
import com.acms.dto.CancelEnrolmentRequest;
import com.acms.dto.EnrolmentResponse;
import com.acms.enums.EnrolmentStatus;
import com.acms.service.EnrolmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Who may call what is defined in SecurityConfig; ownership and district are checked in EnrolmentService. */
@Tag(name = ApiTags.ENROLMENT)
@RestController
@RequiredArgsConstructor
public class EnrolmentController extends BaseController {

    private final EnrolmentService enrolmentService;

    /** A farmer enrols themself: no body, the farmer comes from the token (never from the request). */
    @Operation(
            summary = "Enrol in an offering",
            description = """
                    Farmer enrols (window open, active, prerequisites, land limit, ≥30% paid, place left).

                    - **Permission:** `ENROLMENT_CREATE` (Farmer)
                    - **Scope:** only the caller's own district, offerings or records; any other record gives 403
                    - **Success:** 201
                    - **Errors** (besides 401/403): 404, 409 already enrolled / offering full, 422 window closed / not Active / training missing / land limit / under 30% paid
                    """)
    @PostMapping("/api/offerings/{offeringId}/enrolments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EnrolmentResponse> enrol(@PathVariable Long offeringId) {
        return created(enrolmentService.enrol(offeringId));
    }

    /** The farmers enrolled in an offering (by default only the active enrolments). */
    @Operation(
            summary = "Farmers enrolled in an offering",
            description = """
                    Farmers enrolled in an offering.

                    - **Permission:** `ENROLMENT_LIST_FOR_OFFERING` (Field Officer, Agronomist, Registrar, Admin)
                    - **Scope:** results are limited to the caller's own district, offerings or records
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, enrolledAt, status; default enrolledAt), `direction` (asc or desc; default asc)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/api/offerings/{offeringId}/enrolments")
    public ApiResponse<PagedResponse<EnrolmentResponse>> forOffering(@PathVariable Long offeringId,
                                                       @RequestParam(defaultValue = "ENROLLED") EnrolmentStatus status,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size,
                                                       @RequestParam(defaultValue = "enrolledAt") String sortBy,
                                                       @RequestParam(defaultValue = "asc") String direction) {
        return ok(enrolmentService.findForOffering(offeringId, status, page, size, sortBy, direction));
    }

    @Operation(
            summary = "The farmer's own enrolments",
            description = """
                    The farmer's own enrolments.

                    - **Permission:** `ENROLMENT_READ_OWN` (Farmer)
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, enrolledAt, status; default enrolledAt), `direction` (asc or desc; default desc)
                    - **Success:** 200
                    """)
    @GetMapping("/api/me/enrolments")
    public ApiResponse<PagedResponse<EnrolmentResponse>> mine(@RequestParam(required = false) Long seasonId,
                                                @RequestParam(required = false) EnrolmentStatus status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(defaultValue = "enrolledAt") String sortBy,
                                                @RequestParam(defaultValue = "desc") String direction) {
        return ok(enrolmentService.findMine(seasonId, status, page, size, sortBy, direction));
    }

    @Operation(
            summary = "One enrolment",
            description = """
                    One enrolment.

                    - **Permission:** `ENROLMENT_READ` (Farmer, Field Officer, Agronomist, Registrar, Admin)
                    - **Scope:** only the caller's own district, offerings or records; any other record gives 403
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/api/enrolments/{id}")
    public ApiResponse<EnrolmentResponse> get(@PathVariable Long id) {
        return ok(enrolmentService.findById(id));
    }

    @Operation(
            summary = "Cancel an enrolment",
            description = """
                    Cancel an enrolment (frees the place).

                    - **Permission:** `ENROLMENT_CANCEL` (Farmer, Registrar)
                    - **Scope:** only the caller's own district, offerings or records; any other record gives 403
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404, 400 reason required (Registrar), 409 already cancelled, 422 window closed / season ended
                    """)
    @PostMapping("/api/enrolments/{id}/cancel")
    public ApiResponse<EnrolmentResponse> cancel(@PathVariable Long id,
                                    @Valid @RequestBody(required = false) CancelEnrolmentRequest request) {
        return ok(enrolmentService.cancel(id, request == null ? null : request.reason()));
    }
}
