package com.acms.dto;

import com.acms.entity.Inspection;
import java.math.BigDecimal;

public record InspectionResponse(Long id, Long offeringId, String name, BigDecimal maxScore, BigDecimal weight) {

    public static InspectionResponse from(Inspection inspection) {
        return new InspectionResponse(inspection.getId(), inspection.getOffering().getId(),
                inspection.getName(), inspection.getMaxScore(), inspection.getWeight());
    }
}
