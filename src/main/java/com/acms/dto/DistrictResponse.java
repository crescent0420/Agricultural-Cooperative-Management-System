package com.acms.dto;

import com.acms.entity.District;

public record DistrictResponse(Long id, String name, Long provinceId, String provinceName) {

    public static DistrictResponse from(District district) {
        return new DistrictResponse(
                district.getId(),
                district.getName(),
                district.getProvince() == null ? null : district.getProvince().getId(),
                district.getProvince() == null ? null : district.getProvince().getName());
    }
}
