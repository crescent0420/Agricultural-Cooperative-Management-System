package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.config.ApiTags;
import com.acms.dto.ProvinceResponse;
import com.acms.service.ProvinceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only. A province's districts: GET /api/districts?provinceId={id} */
@Tag(name = ApiTags.REFERENCE_DATA)
@RestController
@RequestMapping("/api/provinces")
@RequiredArgsConstructor
public class ProvinceController extends BaseController {

    private final ProvinceService provinceService;

    @Operation(
            summary = "List provinces",
            description = """
                    The 5 provinces.

                    - **Permission:** `REFERENCE_READ` (all roles)
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, name; default name), `direction` (asc or desc; default asc)
                    - **Success:** 200
                    """)
    @GetMapping
    public ApiResponse<PagedResponse<ProvinceResponse>> list(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @RequestParam(defaultValue = "name") String sortBy,
                                               @RequestParam(defaultValue = "asc") String direction) {
        return ok(provinceService.findAll(page, size, sortBy, direction));
    }
}
