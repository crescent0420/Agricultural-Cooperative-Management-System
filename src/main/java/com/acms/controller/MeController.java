package com.acms.controller;

import com.acms.dto.FarmerResponse;
import com.acms.service.FarmerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final FarmerService farmerService;

    public MeController(FarmerService farmerService) {
        this.farmerService = farmerService;
    }

    @GetMapping("/profile")
    public FarmerResponse getMyProfile() {
        return farmerService.findMyProfile();
    }
}