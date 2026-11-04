
package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.config.ApiTags;
import com.acms.dto.FarmerTrainingRequest;
import com.acms.dto.FarmerTrainingResponse;
import com.acms.dto.TrainingResponse;
import com.acms.service.FarmerTrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = ApiTags.TRAINING_RECORDS)
@RestController
@RequestMapping("/api/farmers/{farmerId}")
public class FarmerTrainingController extends BaseController {

    private final FarmerTrainingService farmerTrainingService;

    public FarmerTrainingController(
            FarmerTrainingService farmerTrainingService) {
        this.farmerTrainingService = farmerTrainingService;
    }

    @Operation(
            summary = "Record a completed training",
            description = """
                    Record a completed training.

                    - **Permission:** `TRAINING_RECORD_MANAGE` (Registrar)
                    - **Success:** 201
                    - **Errors** (besides 401/403): 400, 404, 409 already recorded
                    """)
    @PostMapping("/trainings")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FarmerTrainingResponse> record(
            @PathVariable Long farmerId,
            @Valid @RequestBody FarmerTrainingRequest request) {
        return created(farmerTrainingService.record(farmerId, request));
    }

    @Operation(
            summary = "A farmer's completed trainings",
            description = """
                    A farmer's completed trainings.

                    - **Permission:** `TRAINING_RECORD_READ` (Farmer, Field Officer, Agronomist, Registrar)
                    - **Scope:** results are limited to the caller's own district, offerings or records
                    - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, completedOn, recordedAt; default completedOn), `direction` (asc or desc; default desc)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/trainings")
    public ApiResponse<PagedResponse<FarmerTrainingResponse>> list(
            @PathVariable Long farmerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "completedOn") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return ok(farmerTrainingService.findForFarmer(
                farmerId, page, size, sortBy, direction));
    }

    @Operation(
            summary = "Trainings still missing for a programme",
            description = """
                    Trainings still missing for a programme.

                    - **Permission:** `TRAINING_RECORD_READ` (Farmer, Field Officer, Agronomist, Registrar)
                    - **Scope:** results are limited to the caller's own district, offerings or records
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/missing-prerequisites")
    public ApiResponse<List<TrainingResponse>> missingPrerequisites(
            @PathVariable Long farmerId,
            @RequestParam Long programmeId) {
        return ok(farmerTrainingService.findMissingPrerequisites(
                farmerId, programmeId));
    }
}
