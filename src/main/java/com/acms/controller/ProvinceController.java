package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.dto.ProvinceResponse;
import com.acms.service.ProvinceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only. A province's districts: GET /api/districts?provinceId={id} */
@RestController
@RequestMapping("/api/provinces")
@RequiredArgsConstructor
public class ProvinceController extends BaseController {

    private final ProvinceService provinceService;

    @GetMapping
    public ApiResponse<PagedResponse<ProvinceResponse>> list(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @RequestParam(defaultValue = "name") String sortBy,
                                               @RequestParam(defaultValue = "asc") String direction) {
        return ok(provinceService.findAll(page, size, sortBy, direction));
    }
}
