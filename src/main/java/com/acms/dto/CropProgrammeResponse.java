
package com.acms.dto;

import com.acms.entity.CropProgramme;
import com.acms.entity.Training;
import java.util.Comparator;
import java.util.List;

public record CropProgrammeResponse(
        Long id,
        String name,
        Long districtId,
        String districtName,
        List<TrainingResponse> prerequisites) {

    public static CropProgrammeResponse from(CropProgramme programme) {
        List<TrainingResponse> prerequisites =
                programme.getPrerequisites().stream()
                        .sorted(Comparator.comparing(Training::getName))
                        .map(TrainingResponse::from)
                        .toList();

        return new CropProgrammeResponse(
                programme.getId(),
                programme.getName(),
                programme.getDistrict().getId(),
                programme.getDistrict().getName(),
                prerequisites);
    }
}
