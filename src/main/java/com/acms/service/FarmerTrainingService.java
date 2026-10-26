package com.acms.service;

import com.acms.dto.FarmerTrainingRequest;
import com.acms.dto.FarmerTrainingResponse;
import com.acms.dto.PageResponse;
import com.acms.dto.TrainingResponse;
import com.acms.entity.AppUser;
import com.acms.entity.CropProgramme;
import com.acms.entity.Farmer;
import com.acms.entity.FarmerTraining;
import com.acms.entity.Role;
import com.acms.entity.Training;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.CropProgrammeRepository;
import com.acms.repository.FarmerRepository;
import com.acms.repository.FarmerTrainingRepository;
import com.acms.repository.TrainingRepository;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FarmerTrainingService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "completedOn", "recordedAt");

    private final FarmerRepository farmerRepository;
    private final TrainingRepository trainingRepository;
    private final CropProgrammeRepository programmeRepository;
    private final FarmerTrainingRepository farmerTrainingRepository;
    private final FarmerAccessService farmerAccessService;

    public FarmerTrainingService(
            FarmerRepository farmerRepository,
            TrainingRepository trainingRepository,
            CropProgrammeRepository programmeRepository,
            FarmerTrainingRepository farmerTrainingRepository,
            FarmerAccessService farmerAccessService) {
        this.farmerRepository = farmerRepository;
        this.trainingRepository = trainingRepository;
        this.programmeRepository = programmeRepository;
        this.farmerTrainingRepository = farmerTrainingRepository;
        this.farmerAccessService = farmerAccessService;
    }

    @Transactional
    public FarmerTrainingResponse record(
            Long farmerId, FarmerTrainingRequest request) {

        AppUser currentUser = farmerAccessService.getCurrentUser();

        if (currentUser.getRole() != Role.UNION_REGISTRAR) {
            throw new AccessDeniedException(
                    "You do not have permission to perform this action.");
        }

        Farmer farmer = getFarmerOrThrow(farmerId);

        Training training = trainingRepository.findById(request.trainingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Training with id " + request.trainingId()
                                + " was not found."));

        if (farmerTrainingRepository.existsByFarmerIdAndTrainingId(
                farmerId, training.getId())) {
            throw new ConflictException(
                    "Training '" + training.getName()
                            + "' is already recorded for this farmer.");
        }

        FarmerTraining saved = farmerTrainingRepository.save(
                new FarmerTraining(
                        farmer,
                        training,
                        request.completedOn(),
                        currentUser));

        return FarmerTrainingResponse.from(saved, farmerId);
    }

    @Transactional(readOnly = true)
    public PageResponse<FarmerTrainingResponse> findForFarmer(
            Long farmerId,
            int page,
            int size,
            String sortBy,
            String direction) {

        Farmer farmer = farmerAccessService.requireFarmerAccess(farmerId);

        Pageable pageable = Paging.of(
                page, size, sortBy, direction, SORTABLE_FIELDS);

        return PageResponse.from(
                farmerTrainingRepository.findByFarmerId(
                                farmer.getId(), pageable)
                        .map(record -> FarmerTrainingResponse.from(
                                record, farmer.getId())));
    }

    @Transactional(readOnly = true)
    public List<TrainingResponse> findMissingPrerequisites(
            Long farmerId, Long programmeId) {

        Farmer farmer = farmerAccessService.requireFarmerAccess(farmerId);

        CropProgramme programme = programmeRepository.findById(programmeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Programme with id " + programmeId
                                + " was not found."));

        AppUser currentUser = farmerAccessService.getCurrentUser();

        if (currentUser.getRole() == Role.FIELD_OFFICER
                || currentUser.getRole() == Role.DISTRICT_AGRONOMIST) {

            if (currentUser.getDistrict() == null
                    || !currentUser.getDistrict().getId().equals(
                            programme.getDistrict().getId())) {
                throw new AccessDeniedException(
                        "You do not have permission to perform this action.");
            }
        }

        return farmerTrainingRepository
                .findMissingPrerequisites(farmer.getId(), programme.getId())
                .stream()
                .map(TrainingResponse::from)
                .toList();
    }

    private Farmer getFarmerOrThrow(Long farmerId) {
        return farmerRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer with id " + farmerId + " was not found."));
    }
}
