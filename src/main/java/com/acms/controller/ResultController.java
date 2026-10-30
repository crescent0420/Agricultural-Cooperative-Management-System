package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.dto.CorrectionRequest;
import com.acms.dto.EnterScoresRequest;
import com.acms.dto.ResultCorrectionResponse;
import com.acms.dto.ResultResponse;
import com.acms.dto.ResultSummaryResponse;
import com.acms.dto.ReturnResultRequest;
import com.acms.enums.ResultStatus;
import com.acms.service.ResultService;
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
@RestController
@RequiredArgsConstructor
public class ResultController extends BaseController {

    private final ResultService resultService;

    // ---------- Field Officer ----------

    @PutMapping("/api/enrolments/{enrolmentId}/scores")
    public ApiResponse<ResultResponse> enterScores(@PathVariable Long enrolmentId, @Valid @RequestBody EnterScoresRequest request) {
        return ok(resultService.enterScores(enrolmentId, request.scores()));
    }

    @PostMapping("/api/results/{id}/submit")
    public ApiResponse<ResultResponse> submit(@PathVariable Long id) {
        return ok(resultService.submit(id));
    }

    // ---------- District Agronomist ----------

    @PostMapping("/api/results/{id}/approve")
    public ApiResponse<ResultResponse> approve(@PathVariable Long id) {
        return ok(resultService.approve(id));
    }

    @PostMapping("/api/results/{id}/return")
    public ApiResponse<ResultResponse> returnToDraft(@PathVariable Long id, @Valid @RequestBody ReturnResultRequest request) {
        return ok(resultService.returnToDraft(id, request.reason()));
    }

    @PostMapping("/api/results/{id}/corrections")
    public ApiResponse<ResultResponse> correct(@PathVariable Long id, @Valid @RequestBody CorrectionRequest request) {
        return ok(resultService.correct(id, request.inspectionId(), request.correctedScore(), request.reason()));
    }

    // ---------- reading ----------

    @GetMapping("/api/results/{id}")
    public ApiResponse<ResultResponse> get(@PathVariable Long id) {
        return ok(resultService.findById(id));
    }

    @GetMapping("/api/results/{id}/corrections")
    public ApiResponse<List<ResultCorrectionResponse>> corrections(@PathVariable Long id) {
        return ok(resultService.corrections(id));
    }

    @GetMapping("/api/offerings/{offeringId}/results")
    public ApiResponse<PagedResponse<ResultSummaryResponse>> forOffering(@PathVariable Long offeringId,
                                                           @RequestParam(required = false) ResultStatus status,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size,
                                                           @RequestParam(defaultValue = "id") String sortBy,
                                                           @RequestParam(defaultValue = "asc") String direction) {
        return ok(resultService.forOffering(offeringId, status, page, size, sortBy, direction));
    }

    @GetMapping("/api/me/results")
    public ApiResponse<PagedResponse<ResultSummaryResponse>> mine(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    @RequestParam(defaultValue = "id") String sortBy,
                                                    @RequestParam(defaultValue = "desc") String direction) {
        return ok(resultService.mine(page, size, sortBy, direction));
    }

    @GetMapping("/api/farmers/{farmerId}/results")
    public ApiResponse<PagedResponse<ResultSummaryResponse>> forFarmer(@PathVariable Long farmerId,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size,
                                                         @RequestParam(defaultValue = "id") String sortBy,
                                                         @RequestParam(defaultValue = "desc") String direction) {
        return ok(resultService.forFarmer(farmerId, page, size, sortBy, direction));
    }
}
