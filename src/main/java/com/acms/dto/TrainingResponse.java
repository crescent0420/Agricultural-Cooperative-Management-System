
package com.acms.dto;

import com.acms.entity.Training;

public record TrainingResponse(Long id, String name) {

    public static TrainingResponse from(Training training) {
        return new TrainingResponse(
                training.getId(),
                training.getName());
    }
}
