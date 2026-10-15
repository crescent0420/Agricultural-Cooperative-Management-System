package com.acms.service;

import com.acms.dto.CropProgrammeRenameRequest;
import com.acms.dto.CropProgrammeRequest;
import com.acms.dto.CropProgrammeResponse;
import com.acms.dto.PrerequisiteRequest;
import com.acms.entity.CropProgramme;
import com.acms.entity.District;
import com.acms.entity.Training;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.CropProgrammeRepository;
import com.acms.repository.DistrictRepository;
import com.acms.repository.TrainingRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CropProgrammeService {

    private final CropProgrammeRepository programmeRepository;
    private final DistrictRepository districtRepository;
    private final TrainingRepository trainingRepository;

    public CropProgrammeService(
            CropProgrammeRepository programmeRepository,
            DistrictRepository districtRepository,
            TrainingRepository trainingRepository) {
        this.programmeRepository = programmeRepository;
        this.districtRepository = districtRepository;
        this.trainingRepository = trainingRepository;
    }

    @Transactional
    public CropProgrammeResponse create(CropProgrammeRequest request) {
        District district = districtRepository.findById(request.districtId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "District with id " + request.districtId()
                                + " was not found."));

        String name = request.name().trim();

        if (programmeRepository.existsByDistrictIdAndNameIgnoreCase(
                district.getId(), name)) {
            throw new ConflictException(
                    "Programme '" + name
                            + "' already exists in district "
                            + district.getName() + ".");
        }

        CropProgramme programme = new CropProgramme(name, district);
        return CropProgrammeResponse.from(
                programmeRepository.save(programme));
    }

    @Transactional(readOnly = true)
    public List<CropProgrammeResponse> findAll(Long districtId) {
        Sort sort = Sort.by("name");

        List<CropProgramme> programmes = (districtId == null)
                ? programmeRepository.findAll(sort)
                : programmeRepository.findByDistrictId(districtId, sort);

        return programmes.stream()
                .map(CropProgrammeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CropProgrammeResponse findById(Long id) {
        return CropProgrammeResponse.from(getOrThrow(id));
    }

    @Transactional
    public CropProgrammeResponse rename(
            Long id, CropProgrammeRenameRequest request) {
        CropProgramme programme = getOrThrow(id);
        String name = request.name().trim();
        Long districtId = programme.getDistrict().getId();

        if (programmeRepository.existsByDistrictIdAndNameIgnoreCaseAndIdNot(
                districtId, name, id)) {
            throw new ConflictException(
                    "Programme '" + name
                            + "' already exists in this district.");
        }

        programme.setName(name);
        return CropProgrammeResponse.from(programme);
    }

    @Transactional
    public CropProgrammeResponse addPrerequisite(
            Long programmeId, PrerequisiteRequest request) {
        CropProgramme programme = getOrThrow(programmeId);

        Training training = trainingRepository.findById(request.trainingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Training with id " + request.trainingId()
                                + " was not found."));

        if (programme.hasPrerequisite(training.getId())) {
            throw new ConflictException(
                    "Training '" + training.getName()
                            + "' is already a prerequisite of programme '"
                            + programme.getName() + "'.");
        }

        programme.addPrerequisite(training);
        return CropProgrammeResponse.from(programme);
    }

    @Transactional
    public CropProgrammeResponse removePrerequisite(
            Long programmeId, Long trainingId) {
        CropProgramme programme = getOrThrow(programmeId);

        if (!programme.removePrerequisite(trainingId)) {
            throw new ResourceNotFoundException(
                    "Training with id " + trainingId
                            + " is not a prerequisite of programme "
                            + programmeId + ".");
        }

        return CropProgrammeResponse.from(programme);
    }

    private CropProgramme getOrThrow(Long id) {
        return programmeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Programme with id " + id + " was not found."));
    }
}
