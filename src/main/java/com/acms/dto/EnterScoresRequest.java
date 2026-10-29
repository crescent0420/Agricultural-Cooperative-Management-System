package com.acms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** PUT /api/enrolments/{id}/scores: enters or changes some or all scores of a DRAFT result. */
public record EnterScoresRequest(
        @NotEmpty(message = "scores must contain at least one score")
        List<@Valid ScoreEntry> scores) {
}
