package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.config.ApiTags;
import com.acms.dto.CollectionPermitResponse;
import com.acms.dto.PermitAttemptResponse;
import com.acms.dto.PermitVerificationResponse;
import com.acms.enums.PermitOutcome;
import com.acms.service.PermitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = ApiTags.COLLECTION_PERMITS)
@RestController
@RequiredArgsConstructor
public class PermitController extends BaseController {

    private final PermitService permitService;

    /** 201 when a new permit is issued; 200 with the same permit when it already exists. */
    @Operation(
            summary = "Generate a collection permit",
            description = """
                    Farmer generates the collection permit (every attempt recorded).

                    - **Permission:** `PERMIT_GENERATE` (Farmer)
                    - **Scope:** only the caller's own district, offerings or records; any other record gives 403
                    - **Success:** 201; 200 repeat
                    - **Errors** (besides 401/403): 404, 422 not 100% paid / not active / enrolment cancelled / too late
                    """)
    @PostMapping("/api/enrolments/{enrolmentId}/permit")
    public ResponseEntity<ApiResponse<CollectionPermitResponse>> generate(@PathVariable Long enrolmentId) {
        PermitService.PermitResult result = permitService.generate(enrolmentId);
        return result.created()
                ? ResponseEntity.status(HttpStatus.CREATED).body(created(result.permit()))
                : ResponseEntity.ok(ok("This enrolment already has a permit", result.permit()));
    }

    @Operation(
            summary = "The farmer's own permits",
            description = """
                    The farmer's own permits.

                    - **Permission:** `PERMIT_READ_OWN` (Farmer)
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, issuedAt; default issuedAt), `direction` (asc or desc; default desc)
                    - **Success:** 200
                    """)
    @GetMapping("/api/me/permits")
    public ApiResponse<PagedResponse<CollectionPermitResponse>> mine(@RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size,
                                                                     @RequestParam(defaultValue = "issuedAt") String sortBy,
                                                                     @RequestParam(defaultValue = "desc") String direction) {
        return ok(permitService.mine(page, size, sortBy, direction));
    }

    /** The storekeeper types or scans the permit number. */
    @Operation(
            summary = "Verify a permit number",
            description = """
                    Storekeeper checks a permit number against the current data.

                    - **Permission:** `PERMIT_VERIFY` (Field Officer, Agronomist, Registrar, Finance, Admin)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/api/permits/verify/{permitNumber}")
    public ApiResponse<PermitVerificationResponse> verify(@PathVariable String permitNumber) {
        return ok(permitService.verify(permitNumber));
    }

    @Operation(
            summary = "Every permit attempt",
            description = """
                    Every permit attempt: issued, repeated, refused (with reason).

                    - **Permission:** `PERMIT_ATTEMPT_READ` (Registrar, Admin)
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, attemptedAt; default attemptedAt), `direction` (asc or desc; default desc)
                    - **Success:** 200
                    """)
    @GetMapping("/api/permits/attempts")
    public ApiResponse<PagedResponse<PermitAttemptResponse>> attempts(@RequestParam(required = false) Long farmerId,
                                                                      @RequestParam(required = false) Long enrolmentId,
                                                                      @RequestParam(required = false) PermitOutcome outcome,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "20") int size,
                                                                      @RequestParam(defaultValue = "attemptedAt") String sortBy,
                                                                      @RequestParam(defaultValue = "desc") String direction) {
        return ok(permitService.attempts(farmerId, enrolmentId, outcome, page, size, sortBy, direction));
    }
}
