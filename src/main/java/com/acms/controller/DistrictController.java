package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.dto.DistrictRequest;
import com.acms.dto.DistrictResponse;
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
public class DistrictController extends BaseController {

    private final DistrictService districtService;

    @GetMapping
    public ApiResponse<PagedResponse<DistrictResponse>> list(@RequestParam(required = false) Long provinceId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "50") int size,
                                               @RequestParam(defaultValue = "name") String sortBy,
                                               @RequestParam(defaultValue = "asc") String direction) {
        return ok(districtService.findAll(provinceId, page, size, sortBy, direction));
    }

    @GetMapping("/{id}")
    public ApiResponse<DistrictResponse> get(@PathVariable Long id) {
        return ok(districtService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<DistrictResponse> update(@PathVariable Long id, @Valid @RequestBody DistrictRequest request) {
        return ok(districtService.update(id, request));
    }
}
