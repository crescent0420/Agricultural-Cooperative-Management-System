
package com.acms.dto;

import com.acms.entity.Farmer;
import com.acms.entity.FarmerStatus;

public record FarmerResponse(
        Long id,
        String membershipNumber,
        String nationalId,
        String fullName,
        String phoneNumber,
        FarmerStatus status,
        Long cooperativeId,
        String cooperativeName,
        Long districtId,
        String districtName) {

    public static FarmerResponse from(Farmer farmer) {
        return new FarmerResponse(
                farmer.getId(),
                farmer.getMembershipNumber(),
                farmer.getNationalId(),
                farmer.getFullName(),
                farmer.getPhoneNumber(),
                farmer.getStatus(),
                farmer.getCooperative().getId(),
                farmer.getCooperative().getName(),
                farmer.getCooperative().getDistrict().getId(),
                farmer.getCooperative().getDistrict().getName());
    }
}
