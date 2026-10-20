package com.acms.controller;
import com.acms.dto.FarmerTrainingRequest;
import com.acms.dto.FarmerTrainingResponse;
import com.acms.dto.TrainingResponse;
import com.acms.service.FarmerTrainingService;
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
@RestController
@RequestMapping("/api/farmers/{farmerId}")
public class FarmerTrainingController {
    private final FarmerTrainingService farmerTrainingService;
    public FarmerTrainingController(FarmerTrainingService farmerTrainingService) {
        this.farmerTrainingService = farmerTrainingService;
    }
    @PostMapping("/trainings")
    @ResponseStatus(HttpStatus.CREATED)
    public FarmerTrainingResponse record(
            @PathVariable Long farmerId,
            @Valid @RequestBody FarmerTrainingRequest request) {
        return farmerTrainingService.record(farmerId, request);
    }
    @GetMapping("/trainings")
    public List<FarmerTrainingResponse> list(@PathVariable Long farmerId) {
        return farmerTrainingService.findForFarmer(farmerId);
    }
    @GetMapping("/missing-prerequisites")
    public List<TrainingResponse> missingPrerequisites(
            @PathVariable Long farmerId,
            @RequestParam Long programmeId) {
        return farmerTrainingService.findMissingPrerequisites(farmerId, programmeId);
    }
}
