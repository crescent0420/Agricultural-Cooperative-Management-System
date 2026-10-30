package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.dto.CancelEnrolmentRequest;
import com.acms.dto.EnrolmentResponse;
import com.acms.enums.EnrolmentStatus;
import com.acms.service.EnrolmentService;
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
@RestController
@RequiredArgsConstructor
public class EnrolmentController extends BaseController {

    private final EnrolmentService enrolmentService;

    /** A farmer enrols themself: no body, the farmer comes from the token (never from the request). */
    @PostMapping("/api/offerings/{offeringId}/enrolments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EnrolmentResponse> enrol(@PathVariable Long offeringId) {
        return created(enrolmentService.enrol(offeringId));
    }

    /** The farmers enrolled in an offering (by default only the active enrolments). */
    @GetMapping("/api/offerings/{offeringId}/enrolments")
    public ApiResponse<PagedResponse<EnrolmentResponse>> forOffering(@PathVariable Long offeringId,
                                                       @RequestParam(defaultValue = "ENROLLED") EnrolmentStatus status,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size,
                                                       @RequestParam(defaultValue = "enrolledAt") String sortBy,
                                                       @RequestParam(defaultValue = "asc") String direction) {
        return ok(enrolmentService.findForOffering(offeringId, status, page, size, sortBy, direction));
    }

    @GetMapping("/api/me/enrolments")
    public ApiResponse<PagedResponse<EnrolmentResponse>> mine(@RequestParam(required = false) Long seasonId,
                                                @RequestParam(required = false) EnrolmentStatus status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(defaultValue = "enrolledAt") String sortBy,
                                                @RequestParam(defaultValue = "desc") String direction) {
        return ok(enrolmentService.findMine(seasonId, status, page, size, sortBy, direction));
    }

    @GetMapping("/api/enrolments/{id}")
    public ApiResponse<EnrolmentResponse> get(@PathVariable Long id) {
        return ok(enrolmentService.findById(id));
    }

    @PostMapping("/api/enrolments/{id}/cancel")
    public ApiResponse<EnrolmentResponse> cancel(@PathVariable Long id,
                                    @Valid @RequestBody(required = false) CancelEnrolmentRequest request) {
        return ok(enrolmentService.cancel(id, request == null ? null : request.reason()));
    }
}
