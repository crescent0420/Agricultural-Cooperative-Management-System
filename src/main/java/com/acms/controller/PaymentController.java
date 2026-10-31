package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.dto.PaymentConfirmRequest;
import com.acms.dto.PaymentFailRequest;
import com.acms.dto.PaymentRequest;
import com.acms.dto.PaymentResponse;
import com.acms.service.PaymentService;
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

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController extends BaseController {

    private final PaymentService paymentService;

    /** 201 for a new payment; 200 (with the original payment) when the same request is sent again. */
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> request(@Valid @RequestBody PaymentRequest request) {
        PaymentService.PaymentResult result = paymentService.request(request);
        return result.created()
                ? ResponseEntity.status(HttpStatus.CREATED).body(created(result.payment()))
                : ResponseEntity.ok(ok("Already received: this is the original payment", result.payment()));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<PaymentResponse> confirm(@PathVariable Long id, @Valid @RequestBody PaymentConfirmRequest request) {
        return ok(paymentService.confirm(id, request.providerReference()));
    }

    @PostMapping("/{id}/fail")
    public ApiResponse<PaymentResponse> fail(@PathVariable Long id, @Valid @RequestBody PaymentFailRequest request) {
        return ok(paymentService.fail(id, request.reason()));
    }

    @GetMapping("/{id}")
    public ApiResponse<PaymentResponse> get(@PathVariable Long id) {
        return ok(paymentService.findById(id));
    }
}
