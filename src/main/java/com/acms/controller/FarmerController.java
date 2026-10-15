
package com.acms.controller;

import com.acms.dto.FarmerRegistrationRequest;
import com.acms.dto.FarmerResponse;
import com.acms.dto.FarmerUpdateRequest;
import com.acms.service.FarmerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/farmers")
public class FarmerController {

    private final FarmerService farmerService;

    public FarmerController(FarmerService farmerService) {
        this.farmerService = farmerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FarmerResponse register(
            @Valid @RequestBody FarmerRegistrationRequest request) {
        return farmerService.register(request);
    }

    @GetMapping("/{id}")
    public FarmerResponse get(@PathVariable Long id) {
        return farmerService.findById(id);
    }

    @PutMapping("/{id}")
    public FarmerResponse update(
            @PathVariable Long id,
            @Valid @RequestBody FarmerUpdateRequest request) {
        return farmerService.update(id, request);
    }
}
