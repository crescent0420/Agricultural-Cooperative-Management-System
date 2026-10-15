
package com.acms.controller;

import com.acms.dto.DistrictRequest;
import com.acms.dto.DistrictResponse;
import com.acms.service.DistrictService;

import jakarta.validation.Valid;
import java.util.List;

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
@RequestMapping("/api/districts")
public class DistrictController {

    private final DistrictService districtService;

    public DistrictController(DistrictService districtService) {
        this.districtService = districtService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DistrictResponse create(
            @Valid @RequestBody DistrictRequest request) {
        return districtService.create(request);
    }

    @GetMapping
    public List<DistrictResponse> list() {
        return districtService.findAll();
    }

    @GetMapping("/{id}")
    public DistrictResponse get(@PathVariable Long id) {
        return districtService.findById(id);
    }

    @PutMapping("/{id}")
    public DistrictResponse update(
            @PathVariable Long id,
            @Valid @RequestBody DistrictRequest request) {
        return districtService.update(id, request);
    }
}
