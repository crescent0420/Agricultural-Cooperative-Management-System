package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.config.ApiTags;
import com.acms.dto.PaymentConfirmRequest;
import com.acms.dto.PaymentFailRequest;
import com.acms.dto.PaymentRequest;
import com.acms.dto.PaymentResponse;
import com.acms.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = ApiTags.CONTRIBUTIONS_AND_PAYMENTS)
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController extends BaseController {

    private final PaymentService paymentService;

    /** 201 for a new payment; 200 (with the original payment) when the same request is sent again. */
    @Operation(
            summary = "Record a payment request",
            description = """
                    Record a payment request (idempotent on transactionReference).

                    - **Permission:** `PAYMENT_MANAGE` (Finance)
                    - **Success:** 201; 200 repeat
                    - **Errors** (besides 401/403): 400, 409 same reference with different details, 422 more than still owed
                    """)
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> request(@Valid @RequestBody PaymentRequest request) {
        PaymentService.PaymentResult result = paymentService.request(request);
        return result.created()
                ? ResponseEntity.status(HttpStatus.CREATED).body(created(result.payment()))
                : ResponseEntity.ok(ok("Already received: this is the original payment", result.payment()));
    }

    @Operation(
            summary = "Confirm a payment",
            description = """
                    Confirm a payment (now it counts).

                    - **Permission:** `PAYMENT_MANAGE` (Finance)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 400, 404, 409 failed / other provider reference, 422 would pay more than owed
                    """)
    @PostMapping("/{id}/confirm")
    public ApiResponse<PaymentResponse> confirm(@PathVariable Long id, @Valid @RequestBody PaymentConfirmRequest request) {
        return ok(paymentService.confirm(id, request.providerReference()));
    }

    @Operation(
            summary = "Mark a payment failed",
            description = """
                    Mark a payment failed (reason required).

                    - **Permission:** `PAYMENT_MANAGE` (Finance)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 400, 404, 409 not pending any more
                    """)
    @PostMapping("/{id}/fail")
    public ApiResponse<PaymentResponse> fail(@PathVariable Long id, @Valid @RequestBody PaymentFailRequest request) {
        return ok(paymentService.fail(id, request.reason()));
    }

    @Operation(
            summary = "One payment",
            description = """
                    One payment.

                    - **Permission:** `CONTRIBUTION_READ` (Farmer, Registrar, Finance)
                    - **Scope:** only the caller's own district, offerings or records; any other record gives 403
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/{id}")
    public ApiResponse<PaymentResponse> get(@PathVariable Long id) {
        return ok(paymentService.findById(id));
    }
}
