package com.acms.controller;

import com.acms.dto.CancelEnrolmentRequest;
import com.acms.dto.EnrolmentResponse;
import com.acms.dto.PageResponse;
import com.acms.entity.EnrolmentStatus;
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
public class EnrolmentController {

    private final EnrolmentService enrolmentService;

    /** A farmer enrols themself: no body, the farmer comes from the token (never from the request). */
    @PostMapping("/api/offerings/{offeringId}/enrolments")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrolmentResponse enrol(@PathVariable Long offeringId) {
        return enrolmentService.enrol(offeringId);
    }

    /** The farmers enrolled in an offering (by default only the active enrolments). */
    @GetMapping("/api/offerings/{offeringId}/enrolments")
    public PageResponse<EnrolmentResponse> forOffering(@PathVariable Long offeringId,
                                                       @RequestParam(defaultValue = "ENROLLED") EnrolmentStatus status,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size,
                                                       @RequestParam(defaultValue = "enrolledAt") String sortBy,
                                                       @RequestParam(defaultValue = "asc") String direction) {
        return enrolmentService.findForOffering(offeringId, status, page, size, sortBy, direction);
    }

    @GetMapping("/api/me/enrolments")
    public PageResponse<EnrolmentResponse> mine(@RequestParam(required = false) Long seasonId,
                                                @RequestParam(required = false) EnrolmentStatus status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(defaultValue = "enrolledAt") String sortBy,
                                                @RequestParam(defaultValue = "desc") String direction) {
        return enrolmentService.findMine(seasonId, status, page, size, sortBy, direction);
    }

    @GetMapping("/api/enrolments/{id}")
    public EnrolmentResponse get(@PathVariable Long id) {
        return enrolmentService.findById(id);
    }

    @PostMapping("/api/enrolments/{id}/cancel")
    public EnrolmentResponse cancel(@PathVariable Long id,
                                    @Valid @RequestBody(required = false) CancelEnrolmentRequest request) {
        return enrolmentService.cancel(id, request == null ? null : request.reason());
    }
}
