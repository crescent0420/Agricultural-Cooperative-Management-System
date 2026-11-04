package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.common.PagedResponse;
import com.acms.config.ApiTags;
import com.acms.dto.TrainingRequest;
import com.acms.dto.TrainingResponse;
import com.acms.service.TrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = ApiTags.REFERENCE_DATA)
@RestController
@RequestMapping("/api/trainings")
public class TrainingController extends BaseController {

    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @Operation(
            summary = "Create a training",
            description = """
                    Create a training.

                    - **Permission:** `REFERENCE_MANAGE` (Registrar)
                    - **Success:** 201
                    - **Errors** (besides 401/403): 400, 409 duplicate name
                    """)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TrainingResponse> create(
            @Valid @RequestBody TrainingRequest request) {
        return created(trainingService.create(request));
    }

        @Operation(
                summary = "List trainings",
                description = """
                        List trainings.

                        - **Permission:** `REFERENCE_READ` (all roles)
                        - **Paged:** `page` (from 0), `size` (1-100, default 20), `sortBy` (one of id, name, createdAt; default name), `direction` (asc or desc; default asc)
                        - **Success:** 200
                        """)
        @GetMapping
    public ApiResponse<PagedResponse<TrainingResponse>> list(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @RequestParam(defaultValue = "name") String sortBy,
                                               @RequestParam(defaultValue = "asc") String direction) {
        return ok(trainingService.findAll(page, size, sortBy, direction));
    }
    @Operation(
            summary = "One training",
            description = """
                    One training.

                    - **Permission:** `REFERENCE_READ` (all roles)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 404
                    """)
    @GetMapping("/{id}")
    public ApiResponse<TrainingResponse> get(@PathVariable Long id) {
        return ok(trainingService.findById(id));
    }

    @Operation(
            summary = "Rename a training",
            description = """
                    Rename a training.

                    - **Permission:** `REFERENCE_MANAGE` (Registrar)
                    - **Success:** 200
                    - **Errors** (besides 401/403): 400, 404, 409 duplicate name
                    """)
    @PutMapping("/{id}")
    public ApiResponse<TrainingResponse> rename(
            @PathVariable Long id,
            @Valid @RequestBody TrainingRequest request) {
        return ok(trainingService.rename(id, request));
    }
}
