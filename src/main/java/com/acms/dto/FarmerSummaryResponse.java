package com.acms.dto;
import com.acms.entity.Farmer;
import com.acms.enums.FarmerStatus;
import java.time.Instant;
public record FarmerSummaryResponse(
        Long id,
        String membershipNumber,
        String fullName,
        Long cooperativeId,
        String cooperativeName,
        Long districtId,
        String districtName,
        FarmerStatus status,
        Instant createdAt) {
    public static FarmerSummaryResponse from(Farmer farmer) {
        return new FarmerSummaryResponse(
                farmer.getId(),
                farmer.getMembershipNumber(),
                farmer.getFullName(),
                farmer.getCooperative().getId(),
                farmer.getCooperative().getName(),
                farmer.getCooperative().getDistrict().getId(),
                farmer.getCooperative().getDistrict().getName(),
                farmer.getStatus(),
                farmer.getCreatedAt());
    }
}
