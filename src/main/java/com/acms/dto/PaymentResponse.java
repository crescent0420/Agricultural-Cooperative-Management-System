package com.acms.dto;

import com.acms.entity.Payment;
import com.acms.entity.PaymentMethod;
import com.acms.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long contributionId,
        String transactionReference,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        String providerReference,
        Long requestedById,
        Instant requestedAt,
        Long confirmedById,
        Instant confirmedAt,
        Long failedById,
        Instant failedAt,
        String failureReason) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getObligation().getId(),
                payment.getTransactionReference(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getProviderReference(),
                payment.getRequestedBy().getId(),
                payment.getRequestedAt(),
                payment.getConfirmedBy() == null ? null : payment.getConfirmedBy().getId(),
                payment.getConfirmedAt(),
                payment.getFailedBy() == null ? null : payment.getFailedBy().getId(),
                payment.getFailedAt(),
                payment.getFailureReason());
    }
}
