package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.config.ApiTags;
import com.acms.dto.ProgrammeOfferingRequest;
import com.acms.dto.ProgrammeOfferingResponse;
import com.acms.dto.ProgrammeOfferingUpdateRequest;
import com.acms.service.ProgrammeOfferingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

/** Who may call what is defined in SecurityConfig; which district each user sees is decided in the service. */
@Tag(name = ApiTags.OFFERINGS)
@RestController
@RequestMapping("/api/offerings")
@RequiredArgsConstructor
public class ProgrammeOfferingController extends BaseController {

    private final ProgrammeOfferingService offeringService;

    @Operation(
            summary = "Offer a programme in a season",
            description = """
                    Offer a programme in a season: places, land per participant, Field Officer.

                    - **Permission:** `OFFERING_MANAGE` (Registrar)
                    - **Success:** 201
                    - **Errors** (besides 401/403): 400, 409 already offered in that season, 422 not a Field Officer of the district / season ended / land above the season limit
                    """)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProgrammeOfferingResponse> create(@Valid @RequestBody ProgrammeOfferingRequest request) {
        return created(offeringService.create(request));
    }

    @Operation(
            summary = "List offerings",
            description = """
                    List offerings (district staff and farmers: own district).

                    - **Permission:** `OFFERING_READ` (Farmer, Field Officer, Agronomist, Registrar, Admin)
                    - **Scope:** results are limited to the caller's own district, offerings or records
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, createdAt, maxParticipants, enrolledCount, hectaresPerParticipant; default createdAt), `direction` (asc or desc; default desc)
                    - **Success:** 200
                    """)
    @GetMapping
    public ApiResponse<PagedResponse<ProgrammeOfferingResponse>> list(@RequestParam(required = false) Long seasonId,
                                                        @RequestParam(required = false) Long programmeId,
                                                        @RequestParam(required = false) Long districtId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size,
                                                        @RequestParam(defaultValue = "createdAt") String sortBy,
                                                        @RequestParam(defaultValue = "desc") String direction) {
        return ok(offeringService.findAll(seasonId, programmeId, districtId, page, size, sortBy, direction));
    }

    /** A fixed path wins over /{id}, so "assigned-to-me" is never read as an id. */
    @Operation(
            summary = "The calling Field Officer's offerings",
            description = """
                    The calling Field Officer's offerings.

                    - **Permission:** `OFFERING_READ_ASSIGNED` (Field Officer)
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, createdAt, maxParticipants, enrolledCount, hectaresPerParticipant; default createdAt), `direction` (asc or desc; default desc)
                    - **Success:** 200
                    """)
    @GetMapping("/assigned-to-me")
    public ApiResponse<PagedResponse<ProgrammeOfferingResponse>> assignedToMe(@RequestParam(required = false) Long seasonId,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size,
                                                                @RequestParam(defaultValue = "createdAt") String sortBy,
                                                                @RequestParam(defaultValue = "desc") String direction) {
        return ok(offeringService.findAssignedToCurrentOfficer(seasonId, page, size, sortBy, direction));
    }

    @Operation(
            summary = "One offering with places left",
            description = """
                    One offering with places left.

                    - **Permission:** `OFFERING_READ` (Farmer, Field Officer, Agronomist, Registrar, Admin)
                    - **Scope:** only the caller's own district, offerings or records; any other record gives 403
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/{id}")
    public ApiResponse<ProgrammeOfferingResponse> get(@PathVariable Long id) {
        return ok(offeringService.findById(id));
    }

    @Operation(
            summary = "Change places or Field Officer",
            description = """
                    Change places or Field Officer.

                    - **Permission:** `OFFERING_MANAGE` (Registrar)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 400, 404, 409 fewer places than enrolled / land change after enrolment, 422 officer or land rules
                    """)
    @PutMapping("/{id}")
    public ApiResponse<ProgrammeOfferingResponse> update(@PathVariable Long id,
                                            @Valid @RequestBody ProgrammeOfferingUpdateRequest request) {
        return ok(offeringService.update(id, request));
    }
}
