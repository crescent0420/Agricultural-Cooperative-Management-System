package com.acms.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;
public record FarmerTrainingRequest(
        @NotNull(message = "trainingId is required")
        Long trainingId,
        @NotNull(message = "completedOn is required")
        @PastOrPresent(message = "completedOn cannot be in the future")
        LocalDate completedOn
) {
}
