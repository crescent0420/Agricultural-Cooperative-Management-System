package com.acms.service;

import com.acms.common.BaseService;
import com.acms.common.PagedResponse;
import com.acms.common.Paging;
import com.acms.dto.CropProgrammeRenameRequest;
import com.acms.dto.CropProgrammeRequest;
import com.acms.dto.CropProgrammeResponse;
import com.acms.dto.PrerequisiteRequest;
import com.acms.entity.AppUser;
import com.acms.entity.CropProgramme;
import com.acms.entity.District;
import com.acms.entity.EnrolmentStatus;
import com.acms.entity.Training;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.CropProgrammeRepository;
import com.acms.repository.DistrictRepository;
import com.acms.repository.EnrolmentRepository;
import com.acms.repository.TrainingRepository;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CropProgrammeService extends BaseService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "name", "createdAt");

    private final CropProgrammeRepository programmeRepository;
    private final DistrictRepository districtRepository;
    private final TrainingRepository trainingRepository;
    private final FarmerAccessService farmerAccessService;
    private final EnrolmentRepository enrolmentRepository;

    public CropProgrammeService(
            CropProgrammeRepository programmeRepository,
            DistrictRepository districtRepository,
            TrainingRepository trainingRepository,
            FarmerAccessService farmerAccessService,
            EnrolmentRepository enrolmentRepository) {
        this.programmeRepository = programmeRepository;
        this.districtRepository = districtRepository;
        this.trainingRepository = trainingRepository;
        this.farmerAccessService = farmerAccessService;
        this.enrolmentRepository = enrolmentRepository;
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
    public PagedResponse<CropProgrammeResponse> findAll(
            Long districtId,
            int page,
            int size,
            String sortBy,
            String direction) {

        Pageable pageable = Paging.of(
                page, size, sortBy, direction, SORTABLE_FIELDS);

        Long effectiveDistrictId = districtId;
        AppUser user = farmerAccessService.getCurrentUser();

        if (FarmerAccessService.isDistrictRestricted(user)) {
            Long ownDistrictId = user.getDistrict().getId();

            if (districtId != null && !districtId.equals(ownDistrictId)) {
                return PagedResponse.from(
                        Page.<CropProgrammeResponse>empty(pageable));
            }

            effectiveDistrictId = ownDistrictId;
        }

        Page<CropProgramme> result = (effectiveDistrictId == null)
                ? programmeRepository.findAll(pageable)
                : programmeRepository.findByDistrictId(
                        effectiveDistrictId, pageable);

        return PagedResponse.from(
                result.map(CropProgrammeResponse::from));
    }

    @Transactional(readOnly = true)
    public CropProgrammeResponse findById(Long id) {
        CropProgramme programme = getOrThrow(id);
        AppUser currentUser = farmerAccessService.getCurrentUser();

        checkDistrictAccess(currentUser, programme);

        return CropProgrammeResponse.from(programme);
    }

    @Transactional
    public CropProgrammeResponse rename(
            Long id, CropProgrammeRenameRequest request) {
        CropProgramme programme = getOrThrow(id);
        AppUser currentUser = farmerAccessService.getCurrentUser();

        checkDistrictAccess(currentUser, programme);

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
        requireNoActiveEnrolments(programme);
        AppUser currentUser = farmerAccessService.getCurrentUser();

        checkDistrictAccess(currentUser, programme);

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
        requireNoActiveEnrolments(programme);
        AppUser currentUser = farmerAccessService.getCurrentUser();

        checkDistrictAccess(currentUser, programme);

        if (!programme.removePrerequisite(trainingId)) {
            throw new ResourceNotFoundException(
                    "Training with id " + trainingId
                            + " is not a prerequisite of programme "
                            + programmeId + ".");
        }

        return CropProgrammeResponse.from(programme);
    }

    /** Changing the training rules would be unfair to farmers already enrolled under the old rules. */
    private void requireNoActiveEnrolments(CropProgramme programme) {
        if (enrolmentRepository.existsByOfferingProgrammeIdAndStatus(programme.getId(), EnrolmentStatus.ENROLLED)) {
            throw new ConflictException("The prerequisites of programme '" + programme.getName()
                    + "' cannot change while farmers are enrolled in it.");
        }
    }

    private void checkDistrictAccess(AppUser user, CropProgramme programme) {
        if (FarmerAccessService.isDistrictRestricted(user)
                && (user.getDistrict() == null
                || !user.getDistrict().getId().equals(
                        programme.getDistrict().getId()))) {
            throw new AccessDeniedException(
                    "You do not have permission to perform this action.");
        }
    }

    private CropProgramme getOrThrow(Long id) {
        return programmeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Programme with id " + id + " was not found."));
    }
}