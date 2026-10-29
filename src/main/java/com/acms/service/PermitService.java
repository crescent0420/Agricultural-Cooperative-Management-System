package com.acms.service;

import com.acms.common.ApiException;
import com.acms.common.BaseService;
import com.acms.common.PagedResponse;
import com.acms.dto.CollectionPermitResponse;
import com.acms.dto.PermitAttemptResponse;
import com.acms.dto.PermitVerificationResponse;
import com.acms.entity.AppUser;
import com.acms.entity.CollectionPermit;
import com.acms.entity.ContributionObligation;
import com.acms.entity.Enrolment;
import com.acms.entity.Farmer;
import com.acms.entity.FarmerStatus;
import com.acms.entity.PermitOutcome;
import com.acms.entity.Role;
import com.acms.entity.Season;
import com.acms.exception.BusinessRuleException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.CollectionPermitRepository;
import com.acms.repository.ContributionObligationRepository;
import com.acms.repository.EnrolmentRepository;
import com.acms.repository.PermitAttemptRepository;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Collection permits. A farmer asks for the permit of one of their enrolments; it is issued only when
 *  - the enrolment is still valid (not cancelled),
 *  - the farmer is Active,
 *  - it is before the season's distribution day,
 *  - the season's input contribution is 100% paid (confirmed payments only).
 * Every attempt is recorded in permit_attempts: issued, already issued, or refused with the reason.
 */
@Service
@RequiredArgsConstructor
public class PermitService extends BaseService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "issuedAt");
    private static final Set<String> ATTEMPT_SORT_FIELDS = Set.of("id", "attemptedAt");
    private static final String NO_PERMISSION = "You do not have permission to perform this action.";

    /** No 0/O, 1/I/L: a storekeeper reading the number aloud or typing it cannot confuse characters. */
    private static final char[] ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CollectionPermitRepository permitRepository;
    private final PermitAttemptRepository attemptRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final ContributionObligationRepository obligationRepository;
    private final ContributionService contributionService;
    private final ContributionPolicy contributionPolicy;
    private final PermitAttemptRecorder recorder;
    private final FarmerAccessService farmerAccessService;

    public record PermitResult(CollectionPermitResponse permit, boolean created) {
    }

    // ---------- generate (farmer) ----------

    @Transactional
    public PermitResult generate(Long enrolmentId) {
        AppUser user = farmerAccessService.getCurrentUser();
        Long farmerId = user.getFarmer() == null ? null : user.getFarmer().getId();
        try {
            PermitResult result = issue(user, farmerId, enrolmentId);
            recorder.recordSuccess(user, farmerId, enrolmentId,
                    result.created() ? PermitOutcome.ISSUED : PermitOutcome.ALREADY_ISSUED, result.permit().id());
            return result;
        } catch (ApiException | AccessDeniedException refusal) {
            // Recorded in its own transaction, so it survives the rollback of this one.
            recorder.recordRefusal(user, farmerId, enrolmentId, refusal.getMessage());
            throw refusal;
        }
    }

    /**
     * The checks, in this order:
     *  1. caller is a farmer with a profile, the enrolment exists and is theirs   403 / 404 / 403
     *  2. lock the farmer's row (same lock as enrolment and cancellation)
     *  3. the enrolment is not cancelled                                         422
     *  4. the farmer is Active                                                   422
     *  5. it is before the distribution day                                      422
     *  6. the contribution is 100% paid (confirmed)                              422
     *  7. a permit already exists for this enrolment -> return it               200
     *  8. otherwise issue a new one with a fresh random number                   201
     * The rules are checked even when a permit exists, so a farmer suspended after receiving a permit
     * cannot obtain it again; the storekeeper's verification checks them again at the store.
     */
    private PermitResult issue(AppUser user, Long farmerId, Long enrolmentId) {
        if (user.getRole() != Role.FARMER || farmerId == null) {
            throw new AccessDeniedException(NO_PERMISSION);
        }
        Long ownerId = enrolmentRepository.findFarmerIdById(enrolmentId)
                .orElseThrow(() -> notFound("Enrolment", enrolmentId));
        if (!ownerId.equals(farmerId)) {
            throw new AccessDeniedException(NO_PERMISSION);   // another farmer's enrolment (IDOR)
        }

        // Two requests for the same permit at the same moment queue here; the second finds the first's permit.
        enrolmentRepository.lockFarmer(farmerId);
        Enrolment enrolment = enrolmentRepository.findWithDetailsById(enrolmentId)
                .orElseThrow(() -> notFound("Enrolment", enrolmentId));
        Farmer farmer = enrolment.getFarmer();
        Season season = enrolment.getOffering().getSeason();

        if (!enrolment.isActive()) {
            throw new BusinessRuleException("This enrolment was cancelled, so no permit can be issued.");
        }
        if (farmer.getStatus() != FarmerStatus.ACTIVE) {
            throw new BusinessRuleException("Only Active farmers may collect inputs; this farmer is "
                    + farmer.getStatus() + ".");
        }
        if (!LocalDate.now().isBefore(season.getDistributionDate())) {
            throw new BusinessRuleException("Permits are issued only before the distribution day ("
                    + season.getDistributionDate() + ").");
        }
        contributionPolicy.requireFullyPaid(farmer, season);

        var existing = permitRepository.findByEnrolmentId(enrolmentId);
        if (existing.isPresent()) {
            return new PermitResult(CollectionPermitResponse.from(existing.get()), false);
        }
        CollectionPermit permit = permitRepository.saveAndFlush(
                new CollectionPermit(newPermitNumber(), enrolment, farmer, user));
        return new PermitResult(CollectionPermitResponse.from(permit), true);
    }

    // ---------- verify (storekeeper / staff) ----------

    /** Checks the permit against the CURRENT data: something may have changed since it was issued. */
    @Transactional(readOnly = true)
    public PermitVerificationResponse verify(String permitNumber) {
        String number = permitNumber.trim().toUpperCase(Locale.ROOT);
        CollectionPermit permit = permitRepository.findWithDetailsByPermitNumber(number)
                .orElseThrow(() -> new ResourceNotFoundException("No collection permit has the number " + number + "."));
        Enrolment enrolment = permit.getEnrolment();
        Farmer farmer = permit.getFarmer();
        Season season = enrolment.getOffering().getSeason();

        List<String> problems = new ArrayList<>();
        if (!enrolment.isActive()) {
            problems.add("The enrolment has been cancelled.");
        }
        if (farmer.getStatus() != FarmerStatus.ACTIVE) {
            problems.add("The farmer is " + farmer.getStatus() + ", not Active.");
        }
        if (LocalDate.now().isAfter(season.getDistributionDate())) {
            problems.add("The distribution day (" + season.getDistributionDate() + ") has passed.");
        }
        var obligationId = obligationRepository.findIdByFarmerAndSeason(farmer.getId(), season.getId());
        if (obligationId.isEmpty()) {
            problems.add("No input contribution is recorded for this season.");
        } else {
            ContributionObligation obligation = obligationRepository.findById(obligationId.get()).orElseThrow();
            ContributionBalance balance = contributionService.balanceOf(obligation);
            if (!balance.meets(ContributionPolicy.COLLECTION_MIN_PERCENT)) {
                problems.add("The contribution is not fully paid (" + balance.paidPercent() + "%).");
            }
        }
        return new PermitVerificationResponse(problems.isEmpty(), problems, CollectionPermitResponse.from(permit));
    }

    // ---------- reading ----------

    @Transactional(readOnly = true)
    public PagedResponse<CollectionPermitResponse> mine(int page, int size, String sortBy, String direction) {
        Pageable pageable = pageable(page, size, sortBy, direction, SORTABLE_FIELDS);
        Farmer farmer = farmerAccessService.requireCurrentFarmer();
        return PagedResponse.from(permitRepository.findByFarmerId(farmer.getId(), pageable)
                .map(CollectionPermitResponse::from));
    }

    @Transactional(readOnly = true)
    public PagedResponse<PermitAttemptResponse> attempts(Long farmerId, Long enrolmentId, PermitOutcome outcome,
                                                         int page, int size, String sortBy, String direction) {
        Pageable pageable = pageable(page, size, sortBy, direction, ATTEMPT_SORT_FIELDS);
        return PagedResponse.from(attemptRepository
                .findAll(PermitAttemptRepository.search(farmerId, enrolmentId, outcome), pageable)
                .map(PermitAttemptResponse::from));
    }

    // ---------- helpers ----------

    /** CP-XXXX-XXXX-XXXX from 31 characters: about 8 x 10^17 possibilities, so numbers cannot be guessed. */
    private String newPermitNumber() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder number = new StringBuilder("CP");
            for (int group = 0; group < 3; group++) {
                number.append('-');
                for (int i = 0; i < 4; i++) {
                    number.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
                }
            }
            if (!permitRepository.existsByPermitNumber(number.toString())) {
                return number.toString();
            }
        }
        throw new IllegalStateException("Could not generate a unique permit number.");
    }
}
