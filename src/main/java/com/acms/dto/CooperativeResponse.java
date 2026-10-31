package com.acms.dto;

import com.acms.entity.Cooperative;

public record CooperativeResponse(
        Long id,
        String name,
        Long districtId,
        String districtName) {

    public static CooperativeResponse from(Cooperative cooperative) {
        return new CooperativeResponse(
                cooperative.getId(),
                cooperative.getName(),
                cooperative.getDistrict().getId(),
                cooperative.getDistrict().getName());
    }
}