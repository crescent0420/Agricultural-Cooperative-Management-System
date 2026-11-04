package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.config.ApiTags;
import com.acms.dto.CorrectionRequest;
import com.acms.dto.EnterScoresRequest;
import com.acms.dto.ResultCorrectionResponse;
import com.acms.dto.ResultResponse;
import com.acms.dto.ResultSummaryResponse;
import com.acms.dto.ReturnResultRequest;
import com.acms.enums.ResultStatus;
import com.acms.service.ResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Roles per URL are in SecurityConfig; the offering, district and stage rules are in ResultService. */
@Tag(name = ApiTags.INSPECTIONS_AND_RESULTS)
@RestController
@RequiredArgsConstructor
public class ResultController extends BaseController {

    private final ResultService resultService;

    // ---------- Field Officer ----------

    @Operation(
            summary = "Enter or change draft scores",
            description = """
                    Enter or change draft scores.

                    - **Permission:** `RESULT_ENTER` (Field Officer)
                    - **Scope:** only the caller's own district, offerings or records; any other record gives 403
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404, 400 inspection listed twice, 409 result not a draft, 422 score out of range / enrolment cancelled
                    """)
    @PutMapping("/api/enrolments/{enrolmentId}/scores")
    public ApiResponse<ResultResponse> enterScores(@PathVariable Long enrolmentId, @Valid @RequestBody EnterScoresRequest request) {
        return ok(resultService.enterScores(enrolmentId, request.scores()));
    }

    @Operation(
            summary = "Submit for approval",
            description = """
                    Submit for approval (computes final score and grade).

                    - **Permission:** `RESULT_ENTER` (Field Officer)
                    - **Scope:** only the caller's own district, offerings or records; any other record gives 403
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404, 409 not a draft, 422 no inspections / weights not 100% / a score missing
                    """)
    @PostMapping("/api/results/{id}/submit")
    public ApiResponse<ResultResponse> submit(@PathVariable Long id) {
        return ok(resultService.submit(id));
    }

    // ---------- District Agronomist ----------

    @Operation(
            summary = "Approve and publish a result",
            description = """
                    District Agronomist publishes the result.

                    - **Permission:** `RESULT_APPROVE` (Agronomist)
                    - **Scope:** only the caller's own district, offerings or records; any other record gives 403
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404, 409 not submitted
                    """)
    @PostMapping("/api/results/{id}/approve")
    public ApiResponse<ResultResponse> approve(@PathVariable Long id) {
        return ok(resultService.approve(id));
    }

    @Operation(
            summary = "Return for rework",
            description = """
                    Return for rework (reason required).

                    - **Permission:** `RESULT_APPROVE` (Agronomist)
                    - **Scope:** only the caller's own district, offerings or records; any other record gives 403
                    - **Success:** 200
                    - **Errors** (besides 401/403): 400, 404, 409 not submitted
                    """)
    @PostMapping("/api/results/{id}/return")
    public ApiResponse<ResultResponse> returnToDraft(@PathVariable Long id, @Valid @RequestBody ReturnResultRequest request) {
        return ok(resultService.returnToDraft(id, request.reason()));
    }

    @Operation(
            summary = "Authorised correction of a published score",
            description = """
                    Authorised correction of a published score (original kept).

                    - **Permission:** `RESULT_CORRECT` (Agronomist)
                    - **Scope:** only the caller's own district, offerings or records; any other record gives 403
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404, 400 same score, 409 not published, 422 score out of range
                    """)
    @PostMapping("/api/results/{id}/corrections")
    public ApiResponse<ResultResponse> correct(@PathVariable Long id, @Valid @RequestBody CorrectionRequest request) {
        return ok(resultService.correct(id, request.inspectionId(), request.correctedScore(), request.reason()));
    }

    // ---------- reading ----------

    @Operation(
            summary = "One result with its scores",
            description = """
                    One result with its scores.

                    - **Permission:** `RESULT_READ` (Farmer, Field Officer, Agronomist, Registrar, Admin)
                    - **Scope:** only the caller's own district, offerings or records; any other record gives 403
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/api/results/{id}")
    public ApiResponse<ResultResponse> get(@PathVariable Long id) {
        return ok(resultService.findById(id));
    }

    @Operation(
            summary = "Correction history of a result",
            description = """
                    Correction history of a result.

                    - **Permission:** `RESULT_READ` (Farmer, Field Officer, Agronomist, Registrar, Admin)
                    - **Scope:** results are limited to the caller's own district, offerings or records
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/api/results/{id}/corrections")
    public ApiResponse<List<ResultCorrectionResponse>> corrections(@PathVariable Long id) {
        return ok(resultService.corrections(id));
    }

    @Operation(
            summary = "Results of an offering",
            description = """
                    Results of an offering.

                    - **Permission:** `INSPECTION_READ` (Field Officer, Agronomist, Registrar, Admin)
                    - **Scope:** results are limited to the caller's own district, offerings or records
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, createdAt, finalScore, status; default id), `direction` (asc or desc; default asc)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/api/offerings/{offeringId}/results")
    public ApiResponse<PagedResponse<ResultSummaryResponse>> forOffering(@PathVariable Long offeringId,
                                                           @RequestParam(required = false) ResultStatus status,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size,
                                                           @RequestParam(defaultValue = "id") String sortBy,
                                                           @RequestParam(defaultValue = "asc") String direction) {
        return ok(resultService.forOffering(offeringId, status, page, size, sortBy, direction));
    }

    @Operation(
            summary = "The farmer's own published results",
            description = """
                    The farmer's own published results.

                    - **Permission:** `RESULT_READ_OWN` (Farmer)
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, createdAt, finalScore, status; default id), `direction` (asc or desc; default desc)
                    - **Success:** 200
                    """)
    @GetMapping("/api/me/results")
    public ApiResponse<PagedResponse<ResultSummaryResponse>> mine(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    @RequestParam(defaultValue = "id") String sortBy,
                                                    @RequestParam(defaultValue = "desc") String direction) {
        return ok(resultService.mine(page, size, sortBy, direction));
    }

    @Operation(
            summary = "A farmer's published results",
            description = """
                    A farmer's published results (farmer: only their own).

                    - **Permission:** `RESULT_READ` (Farmer, Field Officer, Agronomist, Registrar, Admin)
                    - **Scope:** results are limited to the caller's own district, offerings or records
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, createdAt, finalScore, status; default id), `direction` (asc or desc; default desc)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/api/farmers/{farmerId}/results")
    public ApiResponse<PagedResponse<ResultSummaryResponse>> forFarmer(@PathVariable Long farmerId,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size,
                                                         @RequestParam(defaultValue = "id") String sortBy,
                                                         @RequestParam(defaultValue = "desc") String direction) {
        return ok(resultService.forFarmer(farmerId, page, size, sortBy, direction));
    }
}
