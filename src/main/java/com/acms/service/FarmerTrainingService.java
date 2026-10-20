package com.acms.service;
import com.acms.dto.FarmerTrainingRequest;
import com.acms.dto.FarmerTrainingResponse;
import com.acms.dto.TrainingResponse;
import com.acms.entity.Farmer;
import com.acms.entity.FarmerTraining;
import com.acms.entity.Training;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.CropProgrammeRepository;
import com.acms.repository.FarmerRepository;
import com.acms.repository.FarmerTrainingRepository;
import com.acms.repository.TrainingRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class FarmerTrainingService {
    private final FarmerRepository farmerRepository;
    private final TrainingRepository trainingRepository;
    private final CropProgrammeRepository programmeRepository;
    private final FarmerTrainingRepository farmerTrainingRepository;
    public FarmerTrainingService(
            FarmerRepository farmerRepository,
            TrainingRepository trainingRepository,
            CropProgrammeRepository programmeRepository,
            FarmerTrainingRepository farmerTrainingRepository
    ) {
        this.farmerRepository = farmerRepository;
        this.trainingRepository = trainingRepository;
        this.programmeRepository = programmeRepository;
        this.farmerTrainingRepository = farmerTrainingRepository;
    }
    @Transactional
    public FarmerTrainingResponse record(Long farmerId, FarmerTrainingRequest request) {
        Farmer farmer = getFarmerOrThrow(farmerId);
        Training training = trainingRepository.findById(request.trainingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Training with id " + request.trainingId() + " was not found."));
        if (farmerTrainingRepository.existsByFarmerIdAndTrainingId(
                farmerId, training.getId())) {
            throw new ConflictException(
                    "Training '" + training.getName()
                            + "' is already recorded for this farmer.");
        }
        FarmerTraining saved = farmerTrainingRepository.save(
                new FarmerTraining(farmer, training, request.completedOn()));
        return FarmerTrainingResponse.from(saved, farmerId);
    }
    @Transactional(readOnly = true)
    public List<FarmerTrainingResponse> findForFarmer(Long farmerId) {
        requireFarmerExists(farmerId);
        Sort newestFirst = Sort.by(
                Sort.Order.desc("completedOn"),
                Sort.Order.desc("id"));
        return farmerTrainingRepository.findByFarmerId(farmerId, newestFirst)
                .stream()
                .map(record -> FarmerTrainingResponse.from(record, farmerId))
                .toList();
    }
    @Transactional(readOnly = true)
    public List<TrainingResponse> findMissingPrerequisites(
            Long farmerId, Long programmeId) {
        requireFarmerExists(farmerId);
        if (!programmeRepository.existsById(programmeId)) {
            throw new ResourceNotFoundException(
                    "Programme with id " + programmeId + " was not found.");
        }
        return farmerTrainingRepository
                .findMissingPrerequisites(farmerId, programmeId)
                .stream()
                .map(TrainingResponse::from)
                .toList();
    }
    private Farmer getFarmerOrThrow(Long farmerId) {
        return farmerRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer with id " + farmerId + " was not found."));
    }
    private void requireFarmerExists(Long farmerId) {
        if (!farmerRepository.existsById(farmerId)) {
            throw new ResourceNotFoundException(
                    "Farmer with id " + farmerId + " was not found.");
        }
    }
}
