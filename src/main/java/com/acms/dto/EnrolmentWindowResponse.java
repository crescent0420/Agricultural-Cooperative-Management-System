package com.acms.dto;

import com.acms.entity.EnrolmentWindow;
import com.acms.entity.WindowStatus;
import java.time.Instant;

public record EnrolmentWindowResponse(
        Long id,
        Long seasonId,
        String seasonName,
        WindowStatus status,
        Instant openedAt,
        String openedBy,
        Instant closedAt,
        String closedBy) {

    public static EnrolmentWindowResponse from(EnrolmentWindow window) {
        return new EnrolmentWindowResponse(
                window.getId(),
                window.getSeason().getId(),
                window.getSeason().getName(),
                window.getStatus(),
                window.getOpenedAt(),
                window.getOpenedBy().getUsername(),
                window.getClosedAt(),
                window.getClosedBy() == null ? null : window.getClosedBy().getUsername());
    }
}
