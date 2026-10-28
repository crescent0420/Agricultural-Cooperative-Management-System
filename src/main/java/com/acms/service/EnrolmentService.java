package com.acms.service;

import com.acms.dto.EnrolmentResponse;
import com.acms.dto.PageResponse;
import com.acms.entity.AppUser;
import com.acms.entity.AuditEventType;
import com.acms.entity.Enrolment;
import com.acms.entity.EnrolmentStatus;
import com.acms.entity.Farmer;
import com.acms.entity.FarmerStatus;
import com.acms.entity.ProgrammeOffering;
import com.acms.entity.Role;
import com.acms.entity.Season;
import com.acms.entity.Training;
import com.acms.exception.BadRequestException;
import com.acms.exception.BusinessRuleException;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.EnrolmentRepository;
import com.acms.repository.EnrolmentSpecifications;
import com.acms.repository.FarmerRepository;
import com.acms.repository.FarmerTrainingRepository;
import com.acms.repository.ProgrammeOfferingRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enrolment of farmers in programme offerings.
 *
 * Everything in enrol() and cancel() runs in ONE database transaction: if any check fails or anything
 * crashes half-way (even after the place was taken), PostgreSQL rolls back every change, so the system
 * is left exactly as it was. Nothing is ever "half enrolled".
 *
 * Row locks are always taken in the same order (farmer -> season -> offering), so two transactions can
 * never wait for each other in a circle (no deadlocks).
 */
@Service
@RequiredArgsConstructor
public class EnrolmentService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "enrolledAt", "status");
    private static final String NO_PERMISSION = "You do not have permission to perform this action.";

    private final EnrolmentRepository enrolmentRepository;
    private final ProgrammeOfferingRepository offeringRepository;
    private final FarmerRepository farmerRepository;
    private final FarmerTrainingRepository farmerTrainingRepository;
    private final SeasonService seasonService;
    private final EnrolmentContributionRule contributionRule;
    private final FarmerAccessService farmerAccessService;
    private final AuditLogService auditLogService;

    // ---------- enrol ----------

    /**
     * The checks, in the order they run, and why that order:
     *  1. caller is a farmer with a farmer profile            403  who is asking comes first
     *  2. the offering exists                                 404
     *  3. lock the farmer's row; offering is in their district 403  (lock: see EnrolmentRepository.lockFarmer)
     *  4. the season's enrolment window is open               422  cheap, and the most common refusal
     *  5. the farmer is Active                                422
     *  6. paid at least 30% of the season contribution        422  (Step 6)
     *  7. every prerequisite training completed               422
     *  8. not already enrolled in this offering               409
     *  9. lock the offering; season land limit respected      422
     * 10. a free place exists, and take it in one statement   409  last: the busiest row is held shortest
     * 11. save the enrolment and the audit record             (same transaction)
     * Refusals 1-8 never touch the offering's row, so a farmer who will be refused anyway never queues
     * behind the farmers competing for the last places.
     */
    @Transactional
    public EnrolmentResponse enrol(Long offeringId) {
        // 1. Only the farmer's id here: the farmer row is read AFTER it is locked (step 3), so it is up to date.
        AppUser user = farmerAccessService.getCurrentUser();
        if (user.getRole() != Role.FARMER || user.getFarmer() == null) {
            throw new AccessDeniedException(NO_PERMISSION);
        }
        Long farmerId = user.getFarmer().getId();

        // 2.
        ProgrammeOffering offering = offeringRepository.findWithDetailsById(offeringId)
                .orElseThrow(() -> offeringNotFound(offeringId));
        Season season = offering.getSeason();

        // 3.
        enrolmentRepository.lockFarmer(farmerId);
        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("The farmer profile was not found."));
        Long farmerDistrictId = farmer.getCooperative().getDistrict().getId();
        if (!farmerDistrictId.equals(offering.getProgramme().getDistrict().getId())) {
            throw new AccessDeniedException(NO_PERMISSION);   // another district's offering: not visible to them
        }

        // 4.
        enrolmentRepository.lockSeasonForShare(season.getId());
        if (season.hasEnded(LocalDate.now()) || !seasonService.isEnrolmentOpen(season.getId())) {
            throw new BusinessRuleException("Enrolment for season '" + season.getName() + "' is not open.");
        }

        // 5.
        if (farmer.getStatus() != FarmerStatus.ACTIVE) {
            throw new BusinessRuleException("Only Active farmers may enrol; this farmer is "
                    + farmer.getStatus() + ".");
        }

        // 6.
        contributionRule.requireEnrolmentThreshold(farmer, season);

        // 7.
        List<Training> missing = farmerTrainingRepository.findMissingPrerequisites(
                farmerId, offering.getProgramme().getId());
        if (!missing.isEmpty()) {
            throw new BusinessRuleException("Prerequisite training not completed: "
                    + missing.stream().map(Training::getName).collect(Collectors.joining(", ")) + ".");
        }

        // 8.
        if (enrolmentRepository.existsByOfferingIdAndFarmerIdAndStatus(
                offeringId, farmerId, EnrolmentStatus.ENROLLED)) {
            throw new ConflictException("This farmer is already enrolled in this offering.");
        }

        // 9. From here on, enrolments of THIS offering run one at a time. The value comes from the
        //    locked row, so a concurrent change by the Registrar cannot slip in between.
        BigDecimal hectares = offeringRepository.lockAndReadHectares(offeringId);
        BigDecimal used = enrolmentRepository.sumHectares(farmerId, season.getId(), EnrolmentStatus.ENROLLED);
        BigDecimal total = (used == null ? BigDecimal.ZERO : used).add(hectares);
        if (total.compareTo(season.getMaxLandHectares()) > 0) {
            throw new BusinessRuleException("This enrolment would bring the farmer's land in season '"
                    + season.getName() + "' to " + total + " ha; the limit is " + season.getMaxLandHectares() + " ha.");
        }

        // 10.
        if (offeringRepository.takePlace(offeringId) == 0) {
            throw new ConflictException("This programme offering is full.");
        }

        // 11. saveAndFlush: the database checks (one active enrolment per farmer and offering) run now.
        Enrolment enrolment = enrolmentRepository.saveAndFlush(new Enrolment(offering, farmer, hectares, user));
        auditLogService.recordInCurrentTransaction(AuditEventType.ENROLMENT_CREATED, true,
                user.getId(), user.getUsername(), "ENROLMENT", enrolment.getId(),
                "Farmer " + farmer.getMembershipNumber() + " enrolled in offering " + offeringId
                        + " ('" + offering.getProgramme().getName() + "', season '" + season.getName() + "')", null);
        return EnrolmentResponse.from(enrolment);
    }

    // ---------- cancel ----------

    /**
     * A farmer may cancel their own enrolment while the window is open.
     * The Union Registrar may cancel any enrolment until the season ends, and must give a reason.
     * The place is given back in the same transaction.
     */
    @Transactional
    public EnrolmentResponse cancel(Long enrolmentId, String reason) {
        AppUser user = farmerAccessService.getCurrentUser();
        Long farmerId = enrolmentRepository.findFarmerIdById(enrolmentId)
                .orElseThrow(() -> enrolmentNotFound(enrolmentId));

        boolean isFarmer = user.getRole() == Role.FARMER;
        if (isFarmer && (user.getFarmer() == null || !user.getFarmer().getId().equals(farmerId))) {
            throw new AccessDeniedException(NO_PERMISSION);   // someone else's enrolment
        }
        if (!isFarmer && user.getRole() != Role.UNION_REGISTRAR) {
            throw new AccessDeniedException(NO_PERMISSION);
        }
        String cleanReason = (reason == null || reason.isBlank()) ? null : reason.trim();
        if (!isFarmer && cleanReason == null) {
            throw new BadRequestException("reason is required when the Registrar cancels an enrolment.");
        }

        // Same lock order as enrol(): farmer first, then the enrolment row, then the offering.
        enrolmentRepository.lockFarmer(farmerId);
        Enrolment enrolment = enrolmentRepository.findByIdForUpdate(enrolmentId)
                .orElseThrow(() -> enrolmentNotFound(enrolmentId));
        if (!enrolment.isActive()) {
            throw new ConflictException("This enrolment is already cancelled.");
        }

        Season season = enrolment.getOffering().getSeason();
        if (season.hasEnded(LocalDate.now())) {
            throw new BusinessRuleException("Season '" + season.getName() + "' has ended; enrolments are final.");
        }
        if (isFarmer && !seasonService.isEnrolmentOpen(season.getId())) {
            throw new BusinessRuleException("The enrolment window is closed; ask the Union Registrar to cancel.");
        }

        Long offeringId = enrolment.getOffering().getId();
        enrolment.cancel(user, cleanReason);
        if (offeringRepository.releasePlace(offeringId) != 1) {
            // Cannot happen while the counter is only changed here and in enrol(); refuse rather than corrupt data.
            throw new IllegalStateException("Offering " + offeringId + " has no taken place to give back.");
        }

        auditLogService.recordInCurrentTransaction(AuditEventType.ENROLMENT_CANCELLED, true,
                user.getId(), user.getUsername(), "ENROLMENT", enrolmentId,
                "Enrolment " + enrolmentId + " in offering " + offeringId + " cancelled"
                        + (cleanReason == null ? "" : ": " + cleanReason), null);
        return EnrolmentResponse.from(enrolment);
    }

    // ---------- reading ----------

    /** A farmer's own enrolments. */
    @Transactional(readOnly = true)
    public PageResponse<EnrolmentResponse> findMine(Long seasonId, EnrolmentStatus status,
                                                    int page, int size, String sortBy, String direction) {
        Pageable pageable = Paging.of(page, size, sortBy, direction, SORTABLE_FIELDS);
        Farmer farmer = farmerAccessService.requireCurrentFarmer();
        return PageResponse.from(enrolmentRepository
                .findAll(EnrolmentSpecifications.search(farmer.getId(), null, seasonId, status), pageable)
                .map(EnrolmentResponse::from));
    }

    /**
     * The farmers enrolled in one offering.
     * Field Officer: only an offering assigned to them, in their district. District Agronomist: their
     * district. Registrar and System Admin: any offering.
     */
    @Transactional(readOnly = true)
    public PageResponse<EnrolmentResponse> findForOffering(Long offeringId, EnrolmentStatus status,
                                                           int page, int size, String sortBy, String direction) {
        Pageable pageable = Paging.of(page, size, sortBy, direction, SORTABLE_FIELDS);
        ProgrammeOffering offering = offeringRepository.findWithDetailsById(offeringId)
                .orElseThrow(() -> offeringNotFound(offeringId));
        requireStaffAccess(farmerAccessService.getCurrentUser(), offering);
        return PageResponse.from(enrolmentRepository
                .findAll(EnrolmentSpecifications.search(null, offeringId, null, status), pageable)
                .map(EnrolmentResponse::from));
    }

    @Transactional(readOnly = true)
    public EnrolmentResponse findById(Long enrolmentId) {
        Enrolment enrolment = enrolmentRepository.findWithDetailsById(enrolmentId)
                .orElseThrow(() -> enrolmentNotFound(enrolmentId));
        AppUser user = farmerAccessService.getCurrentUser();
        if (user.getRole() == Role.FARMER) {
            if (user.getFarmer() == null || !user.getFarmer().getId().equals(enrolment.getFarmer().getId())) {
                throw new AccessDeniedException(NO_PERMISSION);   // another farmer's enrolment (IDOR)
            }
        } else {
            requireStaffAccess(user, enrolment.getOffering());
        }
        return EnrolmentResponse.from(enrolment);
    }

    // ---------- helpers ----------

    private void requireStaffAccess(AppUser user, ProgrammeOffering offering) {
        Long offeringDistrictId = offering.getProgramme().getDistrict().getId();
        boolean allowed = switch (user.getRole()) {
            case UNION_REGISTRAR, SYSTEM_ADMIN -> true;
            case DISTRICT_AGRONOMIST -> user.getDistrict() != null
                    && user.getDistrict().getId().equals(offeringDistrictId);
            case FIELD_OFFICER -> user.getDistrict() != null
                    && user.getDistrict().getId().equals(offeringDistrictId)
                    && offering.getFieldOfficer().getId().equals(user.getId());
            default -> false;
        };
        if (!allowed) {
            throw new AccessDeniedException(NO_PERMISSION);
        }
    }

    private static ResourceNotFoundException offeringNotFound(Long offeringId) {
        return new ResourceNotFoundException("Programme offering with id " + offeringId + " was not found.");
    }

    private static ResourceNotFoundException enrolmentNotFound(Long enrolmentId) {
        return new ResourceNotFoundException("Enrolment with id " + enrolmentId + " was not found.");
    }
}
