package com.acms.service;

import com.acms.common.BaseService;
import com.acms.dto.InspectionListResponse;
import com.acms.dto.InspectionRequest;
import com.acms.dto.InspectionResponse;
import com.acms.entity.AppUser;
import com.acms.entity.Inspection;
import com.acms.entity.ProgrammeOffering;
import com.acms.enums.AuditEventType;
import com.acms.enums.ResultStatus;
import com.acms.exception.BusinessRuleException;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.InspectionRepository;
import com.acms.repository.InspectionResultRepository;
import com.acms.repository.InspectionScoreRepository;
import com.acms.repository.ProgrammeOfferingRepository;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The inspections of an offering, defined by its own Field Officer. Weights are percentages: they may never
 * add up to more than 100, and results can be submitted only when they add up to exactly 100.
 * Once a result of the offering has been submitted, the inspections are frozen.
 */
@Service
@RequiredArgsConstructor
public class InspectionService extends BaseService {

    static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private final InspectionRepository inspectionRepository;
    private final InspectionResultRepository resultRepository;
    private final InspectionScoreRepository scoreRepository;
    private final ProgrammeOfferingRepository offeringRepository;
    private final FarmerAccessService farmerAccessService;
    private final AuditLogService auditLogService;

    @Transactional
    public InspectionResponse create(Long offeringId, InspectionRequest request) {
        ProgrammeOffering offering = findOffering(offeringId);
        AppUser user = farmerAccessService.getCurrentUser();
        OfferingAccess.requireAssignedFieldOfficer(user, offering);

        inspectionRepository.lockOfferingForUpdate(offeringId);
        requireNotFrozen(offeringId);
        String name = request.name().trim();
        if (inspectionRepository.existsByOfferingIdAndNameIgnoreCase(offeringId, name)) {
            throw new ConflictException("This offering already has an inspection called '" + name + "'.");
        }
        requireWeightFits(totalWeight(offeringId).add(request.weight()));

        Inspection inspection = inspectionRepository.saveAndFlush(
                new Inspection(offering, name, request.maxScore(), request.weight(), user));
        audit(AuditEventType.INSPECTION_CREATED, user, inspection.getId(), "Inspection '" + name + "' (max "
                + request.maxScore() + ", weight " + request.weight() + "%) for offering " + offeringId);
        return InspectionResponse.from(inspection);
    }

    /** Only while no result was submitted AND no score was entered for this inspection. */
    @Transactional
    public InspectionResponse update(Long inspectionId, InspectionRequest request) {
        Long offeringId = inspectionRepository.findOfferingIdById(inspectionId)
                .orElseThrow(() -> notFound(inspectionId));
        ProgrammeOffering offering = findOffering(offeringId);
        AppUser user = farmerAccessService.getCurrentUser();
        OfferingAccess.requireAssignedFieldOfficer(user, offering);

        inspectionRepository.lockOfferingForUpdate(offeringId);
        Inspection inspection = inspectionRepository.findById(inspectionId).orElseThrow(() -> notFound(inspectionId));
        requireNotFrozen(offeringId);
        requireNoScores(inspection);
        String name = request.name().trim();
        if (inspectionRepository.existsByOfferingIdAndNameIgnoreCaseAndIdNot(offeringId, name, inspectionId)) {
            throw new ConflictException("This offering already has an inspection called '" + name + "'.");
        }
        requireWeightFits(totalWeight(offeringId).subtract(inspection.getWeight()).add(request.weight()));

        inspection.update(name, request.maxScore(), request.weight());
        inspectionRepository.flush();
        audit(AuditEventType.INSPECTION_UPDATED, user, inspectionId, "Inspection " + inspectionId + " is now '"
                + name + "' (max " + request.maxScore() + ", weight " + request.weight() + "%)");
        return InspectionResponse.from(inspection);
    }

    @Transactional
    public void delete(Long inspectionId) {
        Long offeringId = inspectionRepository.findOfferingIdById(inspectionId)
                .orElseThrow(() -> notFound(inspectionId));
        ProgrammeOffering offering = findOffering(offeringId);
        AppUser user = farmerAccessService.getCurrentUser();
        OfferingAccess.requireAssignedFieldOfficer(user, offering);

        inspectionRepository.lockOfferingForUpdate(offeringId);
        Inspection inspection = inspectionRepository.findById(inspectionId).orElseThrow(() -> notFound(inspectionId));
        requireNotFrozen(offeringId);
        requireNoScores(inspection);
        inspectionRepository.delete(inspection);
        inspectionRepository.flush();
        audit(AuditEventType.INSPECTION_DELETED, user, inspectionId,
                "Inspection '" + inspection.getName() + "' removed from offering " + offeringId);
    }

    @Transactional(readOnly = true)
    public InspectionListResponse list(Long offeringId) {
        ProgrammeOffering offering = findOffering(offeringId);
        OfferingAccess.requireStaffView(farmerAccessService.getCurrentUser(), offering);
        List<Inspection> inspections = inspectionRepository.findByOfferingIdOrderByIdAsc(offeringId);
        BigDecimal total = sumWeights(inspections);
        return new InspectionListResponse(offeringId,
                inspections.stream().map(InspectionResponse::from).toList(),
                total, total.compareTo(HUNDRED) == 0, isFrozen(offeringId));
    }

    // ---------- shared with ResultService ----------

    static BigDecimal sumWeights(List<Inspection> inspections) {
        return inspections.stream().map(Inspection::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ---------- helpers ----------

    private boolean isFrozen(Long offeringId) {
        return resultRepository.existsByEnrolmentOfferingIdAndStatusIn(
                offeringId, EnumSet.of(ResultStatus.SUBMITTED, ResultStatus.PUBLISHED));
    }

    private void requireNotFrozen(Long offeringId) {
        if (isFrozen(offeringId)) {
            throw new ConflictException("Results of this offering have been submitted, so its inspections "
                    + "can no longer change.");
        }
    }

    private void requireNoScores(Inspection inspection) {
        if (scoreRepository.existsByInspectionId(inspection.getId())) {
            throw new ConflictException("Scores have already been entered for inspection '"
                    + inspection.getName() + "'.");
        }
    }

    private BigDecimal totalWeight(Long offeringId) {
        return sumWeights(inspectionRepository.findByOfferingIdOrderByIdAsc(offeringId));
    }

    private static void requireWeightFits(BigDecimal newTotal) {
        if (newTotal.compareTo(HUNDRED) > 0) {
            throw new BusinessRuleException("The weights of an offering's inspections must add up to 100%; "
                    + "this change would make them " + newTotal.stripTrailingZeros().toPlainString() + "%.");
        }
    }

    private ProgrammeOffering findOffering(Long offeringId) {
        return offeringRepository.findWithDetailsById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Programme offering with id " + offeringId + " was not found."));
    }

    private void audit(AuditEventType type, AppUser actor, Long inspectionId, String details) {
        auditLogService.recordInCurrentTransaction(type, true, actor.getId(), actor.getUsername(),
                "INSPECTION", inspectionId, details, null);
    }

    private static ResourceNotFoundException notFound(Long inspectionId) {
        return new ResourceNotFoundException("Inspection with id " + inspectionId + " was not found.");
    }
}
