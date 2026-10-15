
package com.acms.dto;

import jakarta.validation.constraints.NotNull;

public record PrerequisiteRequest(
        @NotNull(message = "trainingId is required")
        Long trainingId) {
}
