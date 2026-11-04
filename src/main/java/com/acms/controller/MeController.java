package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.config.ApiTags;
import com.acms.dto.FarmerResponse;
import com.acms.service.FarmerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = ApiTags.FARMERS)
@RestController
@RequestMapping("/api/me")
public class MeController extends BaseController {

    private final FarmerService farmerService;

    public MeController(FarmerService farmerService) {
        this.farmerService = farmerService;
    }

    @Operation(
            summary = "The farmer's own profile",
            description = """
                    The farmer's own profile.

                    - **Permission:** `PROFILE_READ_OWN` (Farmer)
                    - **Success:** 200
                    """)
    @GetMapping("/profile")
    public ApiResponse<FarmerResponse> getMyProfile() {
        return ok(farmerService.findMyProfile());
    }
}