package com.acms.dto;

import com.acms.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * POST /api/payments. The client chooses transactionReference once per payment and sends the SAME value
 * again if it has to retry (for example after a network failure): the retry then returns the original
 * payment instead of creating a second one.
 */
public record PaymentRequest(
        @NotBlank(message = "transactionReference is required")
        @Size(max = 64, message = "transactionReference must be at most 64 characters")
        @Pattern(regexp = "[A-Za-z0-9._-]+",
                 message = "transactionReference may contain only letters, digits, '.', '_' and '-'")
        String transactionReference,

        @NotNull(message = "contributionId is required")
        Long contributionId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        @Digits(integer = 10, fraction = 2, message = "amount allows up to 10 digits and 2 decimals")
        BigDecimal amount,

        @NotNull(message = "method is required (MOBILE_MONEY or BANK_TRANSFER)")
        PaymentMethod method) {
}
