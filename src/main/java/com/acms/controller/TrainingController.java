package com.acms.controller;

import com.acms.dto.PageResponse;
import com.acms.dto.TrainingRequest;
import com.acms.dto.TrainingResponse;
import com.acms.service.TrainingService;
import jakarta.validation.Valid;
import java.util.List;
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

@RestController
@RequestMapping("/api/trainings")
public class TrainingController {

    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrainingResponse create(
            @Valid @RequestBody TrainingRequest request) {
        return trainingService.create(request);
    }

        @GetMapping
    public PageResponse<TrainingResponse> list(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @RequestParam(defaultValue = "name") String sortBy,
                                               @RequestParam(defaultValue = "asc") String direction) {
        return trainingService.findAll(page, size, sortBy, direction);
    }
    @GetMapping("/{id}")
    public TrainingResponse get(@PathVariable Long id) {
        return trainingService.findById(id);
    }

    @PutMapping("/{id}")
    public TrainingResponse rename(
            @PathVariable Long id,
            @Valid @RequestBody TrainingRequest request) {
        return trainingService.rename(id, request);
    }
}
