package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.dto.ContributionAdjustmentRequest;
import com.acms.dto.ContributionAdjustmentResponse;
import com.acms.dto.ContributionRequest;
import com.acms.dto.ContributionResponse;
import com.acms.dto.PaymentResponse;
import com.acms.service.ContributionService;
import com.acms.service.PaymentService;
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

/** Who may call what is defined in SecurityConfig; a farmer's ownership is checked in the services. */
@RestController
@RequiredArgsConstructor
public class ContributionController extends BaseController {

    private final ContributionService contributionService;
    private final PaymentService paymentService;

    @PostMapping("/api/contributions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ContributionResponse> create(@Valid @RequestBody ContributionRequest request) {
        return created(contributionService.create(request));
    }

    @GetMapping("/api/contributions")
    public ApiResponse<PagedResponse<ContributionResponse>> list(@RequestParam(required = false) Long farmerId,
                                                   @RequestParam(required = false) Long seasonId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @RequestParam(defaultValue = "createdAt") String sortBy,
                                                   @RequestParam(defaultValue = "desc") String direction) {
        return ok(contributionService.findAll(farmerId, seasonId, page, size, sortBy, direction));
    }

    @GetMapping("/api/contributions/{id}")
    public ApiResponse<ContributionResponse> get(@PathVariable Long id) {
        return ok(contributionService.findById(id));
    }

    @PostMapping("/api/contributions/{id}/adjustments")
    public ApiResponse<ContributionResponse> adjust(@PathVariable Long id,
                                       @Valid @RequestBody ContributionAdjustmentRequest request) {
        return ok(contributionService.adjust(id, request));
    }

    @GetMapping("/api/contributions/{id}/adjustments")
    public ApiResponse<PagedResponse<ContributionAdjustmentResponse>> adjustments(@PathVariable Long id,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size,
                                                                    @RequestParam(defaultValue = "adjustedAt") String sortBy,
                                                                    @RequestParam(defaultValue = "desc") String direction) {
        return ok(contributionService.adjustments(id, page, size, sortBy, direction));
    }

    @GetMapping("/api/contributions/{id}/payments")
    public ApiResponse<PagedResponse<PaymentResponse>> payments(@PathVariable Long id,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(defaultValue = "requestedAt") String sortBy,
                                                  @RequestParam(defaultValue = "desc") String direction) {
        return ok(paymentService.findForContribution(id, page, size, sortBy, direction));
    }

    /** A farmer's own contributions with their balances. */
    @GetMapping("/api/me/contributions")
    public ApiResponse<PagedResponse<ContributionResponse>> mine(@RequestParam(required = false) Long seasonId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @RequestParam(defaultValue = "createdAt") String sortBy,
                                                   @RequestParam(defaultValue = "desc") String direction) {
        return ok(contributionService.findMine(seasonId, page, size, sortBy, direction));
    }
}
