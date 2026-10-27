package com.acms.dto;

import com.acms.entity.Province;

public record ProvinceResponse(Long id, String name) {

    public static ProvinceResponse from(Province province) {
        return new ProvinceResponse(province.getId(), province.getName());
    }
}
