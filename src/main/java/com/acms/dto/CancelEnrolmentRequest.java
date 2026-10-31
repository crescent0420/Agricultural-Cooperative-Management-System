package com.acms.dto;

import jakarta.validation.constraints.Size;

/** The reason is optional for a farmer cancelling their own enrolment, and required for the Registrar. */
public record CancelEnrolmentRequest(
        @Size(max = 500, message = "reason must be at most 500 characters")
        String reason) {
}
