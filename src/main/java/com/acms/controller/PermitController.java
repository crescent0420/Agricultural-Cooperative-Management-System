package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.dto.CollectionPermitResponse;
import com.acms.dto.PermitAttemptResponse;
import com.acms.dto.PermitVerificationResponse;
import com.acms.enums.PermitOutcome;
import com.acms.service.PermitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PermitController extends BaseController {

    private final PermitService permitService;

    /** 201 when a new permit is issued; 200 with the same permit when it already exists. */
    @PostMapping("/api/enrolments/{enrolmentId}/permit")
    public ResponseEntity<ApiResponse<CollectionPermitResponse>> generate(@PathVariable Long enrolmentId) {
        PermitService.PermitResult result = permitService.generate(enrolmentId);
        return result.created()
                ? ResponseEntity.status(HttpStatus.CREATED).body(created(result.permit()))
                : ResponseEntity.ok(ok("This enrolment already has a permit", result.permit()));
    }

    @GetMapping("/api/me/permits")
    public ApiResponse<PagedResponse<CollectionPermitResponse>> mine(@RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size,
                                                                     @RequestParam(defaultValue = "issuedAt") String sortBy,
                                                                     @RequestParam(defaultValue = "desc") String direction) {
        return ok(permitService.mine(page, size, sortBy, direction));
    }

    /** The storekeeper types or scans the permit number. */
    @GetMapping("/api/permits/verify/{permitNumber}")
    public ApiResponse<PermitVerificationResponse> verify(@PathVariable String permitNumber) {
        return ok(permitService.verify(permitNumber));
    }

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
