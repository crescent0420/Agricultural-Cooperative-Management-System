package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.config.ApiTags;
import com.acms.dto.ContributionAdjustmentRequest;
import com.acms.dto.ContributionAdjustmentResponse;
import com.acms.dto.ContributionRequest;
import com.acms.dto.ContributionResponse;
import com.acms.dto.PaymentResponse;
import com.acms.service.ContributionService;
import com.acms.service.PaymentService;
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

/** Who may call what is defined in SecurityConfig; a farmer's ownership is checked in the services. */
@Tag(name = ApiTags.CONTRIBUTIONS_AND_PAYMENTS)
@RestController
@RequiredArgsConstructor
public class ContributionController extends BaseController {

    private final ContributionService contributionService;
    private final PaymentService paymentService;

    @Operation(
            summary = "Record a farmer's input contribution for a season",
            description = """
                    Record a farmer's input contribution for a season.

                    - **Permission:** `CONTRIBUTION_MANAGE` (Finance)
                    - **Success:** 201
                    - **Errors** (besides 401/403): 400, 409 already exists for that farmer and season
                    """)
    @PostMapping("/api/contributions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ContributionResponse> create(@Valid @RequestBody ContributionRequest request) {
        return created(contributionService.create(request));
    }

    @Operation(
            summary = "List contributions with balances",
            description = """
                    List contributions with balances.

                    - **Permission:** `CONTRIBUTION_LIST` (Registrar, Finance)
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, createdAt, amountDue; default createdAt), `direction` (asc or desc; default desc)
                    - **Success:** 200
                    """)
    @GetMapping("/api/contributions")
    public ApiResponse<PagedResponse<ContributionResponse>> list(@RequestParam(required = false) Long farmerId,
                                                   @RequestParam(required = false) Long seasonId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @RequestParam(defaultValue = "createdAt") String sortBy,
                                                   @RequestParam(defaultValue = "desc") String direction) {
        return ok(contributionService.findAll(farmerId, seasonId, page, size, sortBy, direction));
    }

    @Operation(
            summary = "One contribution with its balance",
            description = """
                    One contribution with its balance.

                    - **Permission:** `CONTRIBUTION_READ` (Farmer, Registrar, Finance)
                    - **Scope:** only the caller's own district, offerings or records; any other record gives 403
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/api/contributions/{id}")
    public ApiResponse<ContributionResponse> get(@PathVariable Long id) {
        return ok(contributionService.findById(id));
    }

    @Operation(
            summary = "Change the amount owed",
            description = """
                    Change the amount owed (reason recorded).

                    - **Permission:** `CONTRIBUTION_MANAGE` (Finance)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404, 400 same amount, 422 below what is already paid
                    """)
    @PostMapping("/api/contributions/{id}/adjustments")
    public ApiResponse<ContributionResponse> adjust(@PathVariable Long id,
                                       @Valid @RequestBody ContributionAdjustmentRequest request) {
        return ok(contributionService.adjust(id, request));
    }

    @Operation(
            summary = "History of amount changes",
            description = """
                    History of amount changes.

                    - **Permission:** `CONTRIBUTION_LIST` (Registrar, Finance)
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, adjustedAt; default adjustedAt), `direction` (asc or desc; default desc)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/api/contributions/{id}/adjustments")
    public ApiResponse<PagedResponse<ContributionAdjustmentResponse>> adjustments(@PathVariable Long id,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size,
                                                                    @RequestParam(defaultValue = "adjustedAt") String sortBy,
                                                                    @RequestParam(defaultValue = "desc") String direction) {
        return ok(contributionService.adjustments(id, page, size, sortBy, direction));
    }

    @Operation(
            summary = "Payments of a contribution",
            description = """
                    Payments of a contribution.

                    - **Permission:** `CONTRIBUTION_READ` (Farmer, Registrar, Finance)
                    - **Scope:** results are limited to the caller's own district, offerings or records
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, requestedAt, amount, status; default requestedAt), `direction` (asc or desc; default desc)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/api/contributions/{id}/payments")
    public ApiResponse<PagedResponse<PaymentResponse>> payments(@PathVariable Long id,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(defaultValue = "requestedAt") String sortBy,
                                                  @RequestParam(defaultValue = "desc") String direction) {
        return ok(paymentService.findForContribution(id, page, size, sortBy, direction));
    }

    /** A farmer's own contributions with their balances. */
    @Operation(
            summary = "The farmer's own contributions and balances",
            description = """
                    The farmer's own contributions and balances.

                    - **Permission:** `CONTRIBUTION_READ_OWN` (Farmer)
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, createdAt, amountDue; default createdAt), `direction` (asc or desc; default desc)
                    - **Success:** 200
                    """)
    @GetMapping("/api/me/contributions")
    public ApiResponse<PagedResponse<ContributionResponse>> mine(@RequestParam(required = false) Long seasonId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @RequestParam(defaultValue = "createdAt") String sortBy,
                                                   @RequestParam(defaultValue = "desc") String direction) {
        return ok(contributionService.findMine(seasonId, page, size, sortBy, direction));
    }
}
