package com.acms.dto;

import com.acms.entity.GradingBand;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** One band of the grading scale, used both to read and to replace the scale. */
public record GradingBandDto(
        @NotBlank(message = "grade is required")
        @Size(max = 5, message = "grade must be at most 5 characters")
        String grade,

        @NotNull(message = "minScore is required")
        @DecimalMin(value = "0.00", message = "minScore must be between 0 and 100")
        @DecimalMax(value = "100.00", message = "minScore must be between 0 and 100")
        @Digits(integer = 3, fraction = 2, message = "minScore allows up to 2 decimals")
        BigDecimal minScore,

        @Size(max = 100, message = "description must be at most 100 characters")
        String description) {

    public static GradingBandDto from(GradingBand band) {
        return new GradingBandDto(band.getGrade(), band.getMinScore(), band.getDescription());
    }
}
