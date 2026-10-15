

package com.acms.dto;

import com.acms.entity.District;

public record DistrictResponse(Long id, String name) {

    public static DistrictResponse from(District district) {
        return new DistrictResponse(
                district.getId(),
                district.getName()
        );
    }
}
