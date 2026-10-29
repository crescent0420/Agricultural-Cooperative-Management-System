package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.dto.GradingBandDto;
import com.acms.dto.GradingScaleRequest;
import com.acms.service.GradingScaleService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/grading-scale")
@RequiredArgsConstructor
public class GradingScaleController extends BaseController {

    private final GradingScaleService gradingScaleService;

    @GetMapping
    public ApiResponse<List<GradingBandDto>> current() {
        return ok(gradingScaleService.current());
    }

    @PutMapping
    public ApiResponse<List<GradingBandDto>> replace(@Valid @RequestBody GradingScaleRequest request) {
        return ok(gradingScaleService.replace(request));
    }
}
