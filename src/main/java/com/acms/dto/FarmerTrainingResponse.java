package com.acms.dto;
import com.acms.entity.FarmerTraining;
import java.time.LocalDate;
public record FarmerTrainingResponse(
        Long id,
        Long farmerId,
        Long trainingId,
        String trainingName,
        LocalDate completedOn
) {
    public static FarmerTrainingResponse from(FarmerTraining record, Long farmerId) {
        return new FarmerTrainingResponse(
                record.getId(),
                farmerId,
                record.getTraining().getId(),
                record.getTraining().getName(),
                record.getCompletedOn()
        );
    }
}
