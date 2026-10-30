package com.acms.service;

import com.acms.common.BaseService;
import com.acms.common.PagedResponse;
import com.acms.common.Paging;
import com.acms.dto.ProgrammeOfferingRequest;
import com.acms.dto.ProgrammeOfferingResponse;
import com.acms.dto.ProgrammeOfferingUpdateRequest;
import com.acms.entity.AppUser;
import com.acms.entity.CropProgramme;
import com.acms.entity.ProgrammeOffering;
import com.acms.entity.Season;
import com.acms.enums.AuditEventType;
import com.acms.enums.Role;
import com.acms.exception.BusinessRuleException;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.AppUserRepository;
import com.acms.repository.CropProgrammeRepository;
import com.acms.repository.ProgrammeOfferingRepository;
import com.acms.repository.ProgrammeOfferingSpecifications;
import com.acms.repository.SeasonRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Programme offerings: a crop programme offered in one season.
 *  - Only the Union Registrar creates and changes them (SecurityConfig).
 *  - Field Officers and District Agronomists see only their own district's offerings,
 *    and a farmer sees only the offerings of the district their cooperative belongs to.
 */
@Service
@RequiredArgsConstructor
public class ProgrammeOfferingService extends BaseService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "createdAt", "maxParticipants", "enrolledCount", "hectaresPerParticipant");
    private static final String NO_PERMISSION = "You do not have permission to perform this action.";

    private final ProgrammeOfferingRepository offeringRepository;
    private final CropProgrammeRepository programmeRepository;
    private final SeasonRepository seasonRepository;
    private final AppUserRepository appUserRepository;
    private final FarmerAccessService farmerAccessService;
    private final AuditLogService auditLogService;

    // ---------- changes (Union Registrar) ----------

    /**
     * Checks, in this order:
     *  1. the programme, the season and the Field Officer exist          (404)
     *  2. the season has not ended                                       (422)
     *  3. the user is an enabled Field Officer of the programme's district (422)
     *  4. one participant's land fits in the season's land limit         (422)
     *  5. the programme is not already offered in this season            (409)
     */
    @Transactional
    public ProgrammeOfferingResponse create(ProgrammeOfferingRequest request) {
        CropProgramme programme = programmeRepository.findById(request.programmeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Programme with id " + request.programmeId() + " was not found."));
        Season season = seasonRepository.findById(request.seasonId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Season with id " + request.seasonId() + " was not found."));
        AppUser fieldOfficer = findUser(request.fieldOfficerId());

        requireSeasonNotEnded(season);
        requireFieldOfficerOfDistrict(fieldOfficer, programme);
        requireFitsSeasonLandLimit(request.hectaresPerParticipant(), season);
        if (offeringRepository.existsByProgrammeIdAndSeasonId(programme.getId(), season.getId())) {
            throw new ConflictException("Programme '" + programme.getName()
                    + "' is already offered in season '" + season.getName() + "'.");
        }

        // saveAndFlush: write now, so that if two registrars create the same offering at the same moment,
        // the database's unique constraint stops the second one inside this method (409, not a silent duplicate).
        ProgrammeOffering offering = offeringRepository.saveAndFlush(new ProgrammeOffering(
                programme, season, fieldOfficer, request.hectaresPerParticipant(), request.maxParticipants()));

        audit(AuditEventType.OFFERING_CREATED, offering.getId(),
                "Offering " + offering.getId() + ": programme '" + programme.getName() + "' in season '"
                        + season.getName() + "', Field Officer '" + fieldOfficer.getUsername() + "', "
                        + offering.getMaxParticipants() + " places of "
                        + offering.getHectaresPerParticipant() + " ha");
        return ProgrammeOfferingResponse.from(offering);
    }

    /**
     * The row is locked first, so a change of capacity can never overlap with an enrolment (Step 5c).
     *  - The Field Officer may be replaced at any time (same district rules as on creation).
     *  - maxParticipants may not go below the places already taken.
     *  - hectaresPerParticipant is frozen once a farmer has enrolled: their land was checked with the old value.
     */
    @Transactional
    public ProgrammeOfferingResponse update(Long offeringId, ProgrammeOfferingUpdateRequest request) {
        ProgrammeOffering offering = offeringRepository.findByIdForUpdate(offeringId)
                .orElseThrow(() -> notFound(offeringId));
        CropProgramme programme = offering.getProgramme();
        Season season = offering.getSeason();
        AppUser fieldOfficer = findUser(request.fieldOfficerId());

        requireSeasonNotEnded(season);
        requireFieldOfficerOfDistrict(fieldOfficer, programme);
        requireFitsSeasonLandLimit(request.hectaresPerParticipant(), season);

        if (request.maxParticipants() < offering.getEnrolledCount()) {
            throw new ConflictException("maxParticipants cannot be " + request.maxParticipants() + ": "
                    + offering.getEnrolledCount() + " farmers are already enrolled.");
        }
        boolean hectaresChanged = request.hectaresPerParticipant().compareTo(offering.getHectaresPerParticipant()) != 0;
        if (hectaresChanged && offering.hasEnrolments()) {
            throw new ConflictException("hectaresPerParticipant cannot change once farmers have enrolled.");
        }

        String before = describe(offering);
        offering.assignFieldOfficer(fieldOfficer);
        offering.changeHectaresPerParticipant(request.hectaresPerParticipant());
        offering.changeMaxParticipants(request.maxParticipants());
        offeringRepository.flush();   // run the database checks now, inside this method

        audit(AuditEventType.OFFERING_UPDATED, offeringId,
                "Offering " + offeringId + " changed from [" + before + "] to [" + describe(offering) + "]");
        return ProgrammeOfferingResponse.from(offering);
    }

    // ---------- reading ----------

    @Transactional(readOnly = true)
    public PagedResponse<ProgrammeOfferingResponse> findAll(Long seasonId, Long programmeId, Long districtId,
                                                           int page, int size, String sortBy, String direction) {
        Pageable pageable = Paging.of(page, size, sortBy, direction, SORTABLE_FIELDS);
        Long scope = districtScope(farmerAccessService.getCurrentUser());

        Long effectiveDistrictId = districtId;
        if (scope != null) {
            if (districtId != null && !districtId.equals(scope)) {
                // Asking for another district: nothing to see (same behaviour as programmes and farmers).
                return PagedResponse.from(Page.<ProgrammeOfferingResponse>empty(pageable));
            }
            effectiveDistrictId = scope;
        }
        return PagedResponse.from(offeringRepository
                .findAll(ProgrammeOfferingSpecifications.search(seasonId, programmeId, effectiveDistrictId, null),
                        pageable)
                .map(ProgrammeOfferingResponse::from));
    }

    /** A Field Officer's own offerings: the ones they are assigned to. */
    @Transactional(readOnly = true)
    public PagedResponse<ProgrammeOfferingResponse> findAssignedToCurrentOfficer(
            Long seasonId, int page, int size, String sortBy, String direction) {
        Pageable pageable = Paging.of(page, size, sortBy, direction, SORTABLE_FIELDS);
        AppUser officer = farmerAccessService.getCurrentUser();
        if (officer.getRole() != Role.FIELD_OFFICER || officer.getDistrict() == null) {
            throw new AccessDeniedException(NO_PERMISSION);
        }
        // Both conditions: assigned to me AND in my district (in case an officer was later moved).
        return PagedResponse.from(offeringRepository
                .findAll(ProgrammeOfferingSpecifications.search(
                                seasonId, null, officer.getDistrict().getId(), officer.getId()),
                        pageable)
                .map(ProgrammeOfferingResponse::from));
    }

    @Transactional(readOnly = true)
    public ProgrammeOfferingResponse findById(Long offeringId) {
        ProgrammeOffering offering = offeringRepository.findWithDetailsById(offeringId)
                .orElseThrow(() -> notFound(offeringId));
        Long scope = districtScope(farmerAccessService.getCurrentUser());
        if (scope != null && !scope.equals(offering.getProgramme().getDistrict().getId())) {
            throw new AccessDeniedException(NO_PERMISSION);
        }
        return ProgrammeOfferingResponse.from(offering);
    }

    // ---------- rules ----------

    private void requireSeasonNotEnded(Season season) {
        if (season.hasEnded(LocalDate.now())) {
            throw new BusinessRuleException("Season '" + season.getName() + "' ended on " + season.getEndsOn()
                    + "; its offerings can no longer be created or changed.");
        }
    }

    private void requireFieldOfficerOfDistrict(AppUser user, CropProgramme programme) {
        if (user.getRole() != Role.FIELD_OFFICER) {
            throw new BusinessRuleException("User '" + user.getUsername() + "' is not a Field Officer.");
        }
        if (!user.isEnabled()) {
            throw new BusinessRuleException("Field Officer '" + user.getUsername() + "' is disabled.");
        }
        Long programmeDistrictId = programme.getDistrict().getId();
        if (user.getDistrict() == null || !user.getDistrict().getId().equals(programmeDistrictId)) {
            throw new BusinessRuleException("Field Officer '" + user.getUsername()
                    + "' does not belong to district " + programme.getDistrict().getName()
                    + ", which runs programme '" + programme.getName() + "'.");
        }
    }

    /** If one participant's land were bigger than the season's limit, nobody could ever enrol. */
    private void requireFitsSeasonLandLimit(BigDecimal hectaresPerParticipant, Season season) {
        if (hectaresPerParticipant.compareTo(season.getMaxLandHectares()) > 0) {
            throw new BusinessRuleException("hectaresPerParticipant (" + hectaresPerParticipant
                    + ") is larger than the season's land limit per farmer (" + season.getMaxLandHectares() + ").");
        }
    }

    /** The district a user may see, or null for "every district" (Registrar, System Admin). */
    private Long districtScope(AppUser user) {
        if (user.getRole() == Role.FARMER) {
            if (user.getFarmer() == null) {
                throw new AccessDeniedException(NO_PERMISSION);   // an account not yet linked to a farmer
            }
            return user.getFarmer().getCooperative().getDistrict().getId();
        }
        if (FarmerAccessService.isDistrictRestricted(user)) {
            if (user.getDistrict() == null) {
                throw new AccessDeniedException(NO_PERMISSION);
            }
            return user.getDistrict().getId();
        }
        return null;
    }

    // ---------- helpers ----------

    private AppUser findUser(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " was not found."));
    }

    private static String describe(ProgrammeOffering offering) {
        return "officer=" + offering.getFieldOfficer().getUsername()
                + ", ha=" + offering.getHectaresPerParticipant()
                + ", max=" + offering.getMaxParticipants();
    }

    private void audit(AuditEventType type, Long offeringId, String details) {
        AppUser actor = farmerAccessService.getCurrentUser();
        auditLogService.recordInCurrentTransaction(type, true, actor.getId(), actor.getUsername(),
                "PROGRAMME_OFFERING", offeringId, details, null);
    }

    private static ResourceNotFoundException notFound(Long offeringId) {
        return new ResourceNotFoundException("Programme offering with id " + offeringId + " was not found.");
    }
}
