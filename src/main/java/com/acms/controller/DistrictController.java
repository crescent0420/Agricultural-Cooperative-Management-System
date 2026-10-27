package com.acms.controller;

import com.acms.dto.DistrictRequest;
import com.acms.dto.DistrictResponse;
import com.acms.dto.PageResponse;
import com.acms.service.DistrictService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** No POST: the 30 districts are seeded automatically. PUT only corrects a name (Registrar). */
@RestController
@RequestMapping("/api/districts")
@RequiredArgsConstructor
public class DistrictController {

    private final DistrictService districtService;

    @GetMapping
    public PageResponse<DistrictResponse> list(@RequestParam(required = false) Long provinceId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "50") int size,
                                               @RequestParam(defaultValue = "name") String sortBy,
                                               @RequestParam(defaultValue = "asc") String direction) {
        return districtService.findAll(provinceId, page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    public DistrictResponse get(@PathVariable Long id) {
        return districtService.findById(id);
    }

    @PutMapping("/{id}")
    public DistrictResponse update(@PathVariable Long id, @Valid @RequestBody DistrictRequest request) {
        return districtService.update(id, request);
    }
}
