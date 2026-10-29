package com.acms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** PUT /api/grading-scale: replaces the whole scale. Exactly one band must start at 0. */
public record GradingScaleRequest(
        @NotEmpty(message = "bands must contain at least one band")
        @Size(max = 10, message = "at most 10 bands")
        List<@Valid GradingBandDto> bands) {
}
