package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.config.ApiTags;
import com.acms.dto.GradingBandDto;
import com.acms.dto.GradingScaleRequest;
import com.acms.service.GradingScaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = ApiTags.INSPECTIONS_AND_RESULTS)
@RestController
@RequestMapping("/api/grading-scale")
@RequiredArgsConstructor
public class GradingScaleController extends BaseController {

    private final GradingScaleService gradingScaleService;

    @Operation(
            summary = "The grading scale",
            description = """
                    The grading scale (score → grade).

                    - **Permission:** `GRADING_SCALE_READ` (all roles)
                    - **Success:** 200
                    """)
    @GetMapping
    public ApiResponse<List<GradingBandDto>> current() {
        return ok(gradingScaleService.current());
    }

    @Operation(
            summary = "Replace the grading scale",
            description = """
                    Replace the grading scale.

                    - **Permission:** `GRADING_SCALE_MANAGE` (Registrar)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 400 no band at 0 / duplicate grade or minimum
                    """)
    @PutMapping
    public ApiResponse<List<GradingBandDto>> replace(@Valid @RequestBody GradingScaleRequest request) {
        return ok(gradingScaleService.replace(request));
    }
}
