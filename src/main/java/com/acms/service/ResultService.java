package com.acms.service;

import com.acms.common.BaseService;
import com.acms.common.PagedResponse;
import com.acms.common.Paging;
import com.acms.dto.ResultCorrectionResponse;
import com.acms.dto.ResultResponse;
import com.acms.dto.ResultSummaryResponse;
import com.acms.dto.ScoreEntry;
import com.acms.dto.ScoreLine;
import com.acms.entity.AppUser;
import com.acms.entity.Enrolment;
import com.acms.entity.Farmer;
import com.acms.entity.Inspection;
import com.acms.entity.InspectionResult;
import com.acms.entity.InspectionScore;
import com.acms.entity.ProgrammeOffering;
import com.acms.entity.ResultCorrection;
import com.acms.enums.AuditEventType;
import com.acms.enums.ResultStatus;
import com.acms.enums.Role;
import com.acms.exception.BadRequestException;
import com.acms.exception.BusinessRuleException;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.EnrolmentRepository;
import com.acms.repository.InspectionRepository;
import com.acms.repository.InspectionResultRepository;
import com.acms.repository.InspectionResultSpecifications;
import com.acms.repository.InspectionScoreRepository;
import com.acms.repository.ProgrammeOfferingRepository;
import com.acms.repository.ResultCorrectionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inspection results, from the first score to publication and later corrections.
 *
 *   step                         who                                        stage needed
 *   enter / change scores        the offering's Field Officer               DRAFT
 *   submit                       the offering's Field Officer               DRAFT     -> SUBMITTED
 *   return for rework            District Agronomist of that district       SUBMITTED -> DRAFT
 *   approve = publish            District Agronomist of that district       SUBMITTED -> PUBLISHED
 *   correct                      District Agronomist of that district       PUBLISHED (stays PUBLISHED)
 *
 * final score = sum over the inspections of (score / maxScore) x weight   -> 0 to 100, 2 decimals
 */
@Service
@RequiredArgsConstructor
public class ResultService extends BaseService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "createdAt", "finalScore", "status");
    private static final String NO_PERMISSION = "You do not have permission to perform this action.";

    private final InspectionResultRepository resultRepository;
    private final InspectionScoreRepository scoreRepository;
    private final ResultCorrectionRepository correctionRepository;
    private final InspectionRepository inspectionRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final ProgrammeOfferingRepository offeringRepository;
    private final GradingScaleService gradingScale;
    private final FarmerAccessService farmerAccessService;
    private final AuditLogService auditLogService;

    // ---------- Field Officer ----------

    /** Enters or changes scores of a farmer's DRAFT result (the result is created on the first score). */
    @Transactional
    public ResultResponse enterScores(Long enrolmentId, List<ScoreEntry> entries) {
        Enrolment enrolment = enrolmentRepository.findWithDetailsById(enrolmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrolment with id " + enrolmentId + " was not found."));
        ProgrammeOffering offering = enrolment.getOffering();
        AppUser user = farmerAccessService.getCurrentUser();
        OfferingAccess.requireAssignedFieldOfficer(user, offering);
        if (!enrolment.isActive()) {
            throw new BusinessRuleException("This enrolment was cancelled; no scores can be entered.");
        }

        inspectionRepository.lockOfferingForShare(offering.getId());
        Map<Long, Inspection> inspections = inspectionsById(offering.getId());
        Set<Long> seen = new HashSet<>();
        for (ScoreEntry entry : entries) {
            if (!seen.add(entry.inspectionId())) {
                throw new BadRequestException("Inspection " + entry.inspectionId() + " appears twice.");
            }
            requireValidScore(inspections, entry.inspectionId(), entry.score());
        }

        InspectionResult result = resultRepository.findByEnrolmentIdForUpdate(enrolmentId)
                .orElseGet(() -> resultRepository.saveAndFlush(new InspectionResult(enrolment, user)));
        requireStage(result, ResultStatus.DRAFT, "Scores can be changed only while the result is a DRAFT");

        Map<Long, InspectionScore> existing = scoreRepository.findByResultId(result.getId()).stream()
                .collect(Collectors.toMap(s -> s.getInspection().getId(), Function.identity()));
        for (ScoreEntry entry : entries) {
            InspectionScore score = existing.get(entry.inspectionId());
            if (score == null) {
                scoreRepository.save(new InspectionScore(result, inspections.get(entry.inspectionId()),
                        entry.score(), user));
            } else {
                score.change(entry.score(), user);
            }
        }
        scoreRepository.flush();
        audit(AuditEventType.SCORES_ENTERED, user, result.getId(), entries.size() + " score(s) entered for farmer "
                + enrolment.getFarmer().getMembershipNumber() + " in offering " + offering.getId());
        return detail(result);
    }

    /** Needs a score for every inspection, and weights that add up to exactly 100. */
    @Transactional
    public ResultResponse submit(Long resultId) {
        Long offeringId = resultRepository.findOfferingIdById(resultId).orElseThrow(() -> notFound(resultId));
        inspectionRepository.lockOfferingForShare(offeringId);
        InspectionResult result = resultRepository.findByIdForUpdate(resultId).orElseThrow(() -> notFound(resultId));
        AppUser user = farmerAccessService.getCurrentUser();
        OfferingAccess.requireAssignedFieldOfficer(user, result.getEnrolment().getOffering());
        requireStage(result, ResultStatus.DRAFT, "Only a DRAFT result can be submitted");

        List<Inspection> inspections = inspectionRepository.findByOfferingIdOrderByIdAsc(offeringId);
        if (inspections.isEmpty()) {
            throw new BusinessRuleException("This offering has no inspections yet.");
        }
        BigDecimal totalWeight = InspectionService.sumWeights(inspections);
        if (totalWeight.compareTo(InspectionService.HUNDRED) != 0) {
            throw new BusinessRuleException("The inspection weights of this offering add up to "
                    + totalWeight.stripTrailingZeros().toPlainString() + "%; they must add up to exactly 100%.");
        }
        Map<Long, BigDecimal> scores = currentScores(result);
        List<String> missing = inspections.stream().filter(i -> !scores.containsKey(i.getId()))
                .map(Inspection::getName).toList();
        if (!missing.isEmpty()) {
            throw new BusinessRuleException("Scores are missing for: " + String.join(", ", missing) + ".");
        }

        BigDecimal finalScore = finalScore(inspections, scores);
        result.submit(user, finalScore, gradingScale.gradeFor(finalScore));
        resultRepository.flush();
        audit(AuditEventType.RESULT_SUBMITTED, user, resultId,
                "Result " + resultId + " submitted: " + finalScore + " (" + result.getGrade() + ")");
        return detail(result);
    }

    // ---------- District Agronomist ----------

    @Transactional
    public ResultResponse approve(Long resultId) {
        InspectionResult result = lockForAgronomist(resultId);
        requireStage(result, ResultStatus.SUBMITTED, "Only a SUBMITTED result can be approved");
        AppUser user = farmerAccessService.getCurrentUser();

        // The grade comes from the scale in force at publication; the farmer then keeps it.
        List<Inspection> inspections = inspectionRepository.findByOfferingIdOrderByIdAsc(
                result.getEnrolment().getOffering().getId());
        BigDecimal finalScore = finalScore(inspections, currentScores(result));
        result.publish(user, finalScore, gradingScale.gradeFor(finalScore));
        resultRepository.flush();
        audit(AuditEventType.RESULT_PUBLISHED, user, resultId,
                "Result " + resultId + " approved and published: " + finalScore + " (" + result.getGrade() + ")");
        return detail(result);
    }

    @Transactional
    public ResultResponse returnToDraft(Long resultId, String reason) {
        InspectionResult result = lockForAgronomist(resultId);
        requireStage(result, ResultStatus.SUBMITTED, "Only a SUBMITTED result can be returned");
        AppUser user = farmerAccessService.getCurrentUser();
        result.returnToDraft();
        resultRepository.flush();
        audit(AuditEventType.RESULT_RETURNED, user, resultId,
                "Result " + resultId + " returned to the Field Officer: " + reason.trim());
        return detail(result);
    }

    /**
     * The authorised correction of a published result. The original score stays untouched in
     * inspection_scores; the correction row records original and corrected values, reason, who and when.
     */
    @Transactional
    public ResultResponse correct(Long resultId, Long inspectionId, BigDecimal correctedScore, String reason) {
        InspectionResult result = lockForAgronomist(resultId);
        requireStage(result, ResultStatus.PUBLISHED,
                "Only a PUBLISHED result is corrected (a SUBMITTED one is returned to DRAFT instead)");
        AppUser user = farmerAccessService.getCurrentUser();

        List<Inspection> inspectionList = inspectionRepository.findByOfferingIdOrderByIdAsc(
                result.getEnrolment().getOffering().getId());
        Map<Long, Inspection> inspections = inspectionList.stream()
                .collect(Collectors.toMap(Inspection::getId, Function.identity()));
        requireValidScore(inspections, inspectionId, correctedScore);

        Map<Long, BigDecimal> scores = currentScores(result);
        BigDecimal previous = scores.get(inspectionId);
        if (previous.compareTo(correctedScore) == 0) {
            throw new BadRequestException("The score is already " + previous + ".");
        }
        scores.put(inspectionId, correctedScore);
        BigDecimal newFinal = finalScore(inspectionList, scores);
        String newGrade = gradingScale.gradeFor(newFinal);

        correctionRepository.save(new ResultCorrection(result, inspections.get(inspectionId),
                previous, correctedScore, result.getFinalScore(), newFinal, result.getGrade(), newGrade,
                reason.trim(), user));
        String oldSummary = result.getFinalScore() + " (" + result.getGrade() + ")";
        result.applyCorrection(newFinal, newGrade);
        resultRepository.flush();   // the correction row is inserted before the result is updated
        audit(AuditEventType.RESULT_CORRECTED, user, resultId, "Inspection '" + inspections.get(inspectionId).getName()
                + "' corrected from " + previous + " to " + correctedScore + "; final " + oldSummary + " -> "
                + newFinal + " (" + newGrade + "): " + reason.trim());
        return detail(result);
    }

    // ---------- reading ----------

    /** Staff by scope; a farmer only their own PUBLISHED result. */
    @Transactional(readOnly = true)
    public ResultResponse findById(Long resultId) {
        InspectionResult result = resultRepository.findWithDetailsById(resultId).orElseThrow(() -> notFound(resultId));
        requireCanRead(result);
        return detail(result);
    }

    @Transactional(readOnly = true)
    public List<ResultCorrectionResponse> corrections(Long resultId) {
        InspectionResult result = resultRepository.findWithDetailsById(resultId).orElseThrow(() -> notFound(resultId));
        requireCanRead(result);
        return correctionRepository.findByResultIdOrderByIdAsc(resultId).stream()
                .map(ResultCorrectionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ResultSummaryResponse> forOffering(Long offeringId, ResultStatus status,
                                                           int page, int size, String sortBy, String direction) {
        Pageable pageable = Paging.of(page, size, sortBy, direction, SORTABLE_FIELDS);
        ProgrammeOffering offering = offeringRepository.findWithDetailsById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Programme offering with id " + offeringId + " was not found."));
        OfferingAccess.requireStaffView(farmerAccessService.getCurrentUser(), offering);
        return page(InspectionResultSpecifications.search(offeringId, null, null, null, status), pageable);
    }

    /** A farmer's own PUBLISHED results. */
    @Transactional(readOnly = true)
    public PagedResponse<ResultSummaryResponse> mine(int page, int size, String sortBy, String direction) {
        Pageable pageable = Paging.of(page, size, sortBy, direction, SORTABLE_FIELDS);
        Farmer farmer = farmerAccessService.requireCurrentFarmer();
        return page(InspectionResultSpecifications.search(null, farmer.getId(), null, null, ResultStatus.PUBLISHED),
                pageable);
    }

    /**
     * GET /api/farmers/{farmerId}/results. requireFarmerAccess stops a farmer who changes the id in the URL
     * (403) before anything is read; staff then see only what their role and district allow.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ResultSummaryResponse> forFarmer(Long farmerId, int page, int size,
                                                         String sortBy, String direction) {
        Pageable pageable = Paging.of(page, size, sortBy, direction, SORTABLE_FIELDS);
        farmerAccessService.requireFarmerAccess(farmerId);
        AppUser user = farmerAccessService.getCurrentUser();
        var spec = switch (user.getRole()) {
            case FARMER -> InspectionResultSpecifications.search(null, farmerId, null, null, ResultStatus.PUBLISHED);
            case FIELD_OFFICER -> InspectionResultSpecifications.search(null, farmerId,
                    districtOf(user), user.getId(), null);
            case DISTRICT_AGRONOMIST -> InspectionResultSpecifications.search(null, farmerId,
                    districtOf(user), null, null);
            case UNION_REGISTRAR, SYSTEM_ADMIN -> InspectionResultSpecifications.search(null, farmerId, null, null, null);
            default -> throw new AccessDeniedException(NO_PERMISSION);
        };
        return page(spec, pageable);
    }

    // ---------- calculation ----------

    /** Sum of (score / maxScore) x weight, rounded to 2 decimals at the very end only. */
    static BigDecimal finalScore(List<Inspection> inspections, Map<Long, BigDecimal> scores) {
        BigDecimal total = BigDecimal.ZERO;
        for (Inspection inspection : inspections) {
            total = total.add(scores.get(inspection.getId())
                    .multiply(inspection.getWeight())
                    .divide(inspection.getMaxScore(), 10, RoundingMode.HALF_UP));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /** The original scores with every correction applied in order: the values that count now. */
    private Map<Long, BigDecimal> currentScores(InspectionResult result) {
        Map<Long, BigDecimal> scores = new HashMap<>();
        for (InspectionScore score : scoreRepository.findByResultId(result.getId())) {
            scores.put(score.getInspection().getId(), score.getScore());
        }
        for (ResultCorrection correction : correctionRepository.findByResultIdOrderByIdAsc(result.getId())) {
            scores.put(correction.getInspection().getId(), correction.getCorrectedScore());
        }
        return scores;
    }

    // ---------- helpers ----------

    private ResultResponse detail(InspectionResult result) {
        List<Inspection> inspections = inspectionRepository.findByOfferingIdOrderByIdAsc(
                result.getEnrolment().getOffering().getId());
        Map<Long, BigDecimal> original = new HashMap<>();
        for (InspectionScore score : scoreRepository.findByResultId(result.getId())) {
            original.put(score.getInspection().getId(), score.getScore());
        }
        List<ResultCorrection> corrections = correctionRepository.findByResultIdOrderByIdAsc(result.getId());
        Map<Long, BigDecimal> current = new LinkedHashMap<>(original);
        corrections.forEach(c -> current.put(c.getInspection().getId(), c.getCorrectedScore()));

        List<ScoreLine> lines = inspections.stream().map(i -> new ScoreLine(i.getId(), i.getName(),
                i.getMaxScore(), i.getWeight(), original.get(i.getId()), current.get(i.getId()),
                corrections.stream().anyMatch(c -> c.getInspection().getId().equals(i.getId())))).toList();
        return ResultResponse.from(result, lines, corrections.size());
    }

    private PagedResponse<ResultSummaryResponse> page(
            org.springframework.data.jpa.domain.Specification<InspectionResult> spec, Pageable pageable) {
        return PagedResponse.from(resultRepository.findAll(spec, pageable).map(ResultSummaryResponse::from));
    }

    private void requireCanRead(InspectionResult result) {
        AppUser user = farmerAccessService.getCurrentUser();
        if (user.getRole() == Role.FARMER) {
            boolean own = user.getFarmer() != null
                    && user.getFarmer().getId().equals(result.getEnrolment().getFarmer().getId());
            if (!own || result.getStatus() != ResultStatus.PUBLISHED) {
                throw new AccessDeniedException(NO_PERMISSION);   // not theirs, or not published yet
            }
            return;
        }
        OfferingAccess.requireStaffView(user, result.getEnrolment().getOffering());
    }

    private InspectionResult lockForAgronomist(Long resultId) {
        InspectionResult result = resultRepository.findByIdForUpdate(resultId).orElseThrow(() -> notFound(resultId));
        OfferingAccess.requireDistrictAgronomist(farmerAccessService.getCurrentUser(),
                result.getEnrolment().getOffering());
        return result;
    }

    private static void requireStage(InspectionResult result, ResultStatus needed, String message) {
        if (result.getStatus() != needed) {
            String hint = result.getStatus() == ResultStatus.PUBLISHED
                    ? " It is PUBLISHED: only an authorised correction can change it." : "";
            throw new ConflictException(message + "; this result is " + result.getStatus() + "." + hint);
        }
    }

    private static void requireValidScore(Map<Long, Inspection> inspections, Long inspectionId, BigDecimal score) {
        Inspection inspection = inspections.get(inspectionId);
        if (inspection == null) {
            throw new BusinessRuleException("Inspection " + inspectionId + " does not belong to this offering.");
        }
        if (score.compareTo(inspection.getMaxScore()) > 0) {
            throw new BusinessRuleException("The score for '" + inspection.getName() + "' must be between 0 and "
                    + inspection.getMaxScore() + "; got " + score + ".");
        }
    }

    private Map<Long, Inspection> inspectionsById(Long offeringId) {
        return inspectionRepository.findByOfferingIdOrderByIdAsc(offeringId).stream()
                .collect(Collectors.toMap(Inspection::getId, Function.identity()));
    }

    private static Long districtOf(AppUser user) {
        if (user.getDistrict() == null) {
            throw new AccessDeniedException(NO_PERMISSION);
        }
        return user.getDistrict().getId();
    }

    private void audit(AuditEventType type, AppUser actor, Long resultId, String details) {
        auditLogService.recordInCurrentTransaction(type, true, actor.getId(), actor.getUsername(),
                "INSPECTION_RESULT", resultId, details, null);
    }

    private static ResourceNotFoundException notFound(Long resultId) {
        return new ResourceNotFoundException("Result with id " + resultId + " was not found.");
    }
}
