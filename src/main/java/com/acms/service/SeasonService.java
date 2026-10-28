package com.acms.service;

import com.acms.common.BaseService;
import com.acms.common.PagedResponse;
import com.acms.common.Paging;
import com.acms.dto.EnrolmentWindowResponse;
import com.acms.dto.SeasonRequest;
import com.acms.dto.SeasonResponse;
import com.acms.entity.AppUser;
import com.acms.entity.AuditEventType;
import com.acms.entity.EnrolmentWindow;
import com.acms.entity.Season;
import com.acms.entity.WindowStatus;
import com.acms.exception.BadRequestException;
import com.acms.exception.BusinessRuleException;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.AppUserRepository;
import com.acms.repository.EnrolmentWindowRepository;
import com.acms.repository.SeasonRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeasonService extends BaseService {

    private static final Set<String> SEASON_SORT_FIELDS = Set.of("id", "name", "startsOn", "endsOn", "createdAt");
    private static final Set<String> WINDOW_SORT_FIELDS = Set.of("id", "openedAt", "closedAt");

    private final SeasonRepository seasonRepository;
    private final EnrolmentWindowRepository windowRepository;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;

    // ---------- seasons ----------

    @Transactional
    public SeasonResponse create(SeasonRequest request, Long actorUserId) {
        String name = request.name().trim();
        validateDates(request);
        if (seasonRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Season '" + name + "' already exists.");
        }
        Season saved = seasonRepository.save(new Season(name, request.startsOn(), request.endsOn(),
                request.maxLandHectares(), request.distributionDate()));
        audit(AuditEventType.SEASON_CREATED, actorUserId, saved.getId(), "Season '" + name + "' created");
        return toResponse(saved, null);
    }

    /** Allowed only before the first enrolment window: after that, farmers' enrolments depend on these values. */
    @Transactional
    public SeasonResponse update(Long seasonId, SeasonRequest request, Long actorUserId) {
        Season season = seasonRepository.findByIdForUpdate(seasonId).orElseThrow(() -> notFound(seasonId));
        if (windowRepository.existsBySeasonId(seasonId)) {
            throw new ConflictException("A season cannot be changed once an enrolment window has been opened.");
        }
        String name = request.name().trim();
        validateDates(request);
        if (seasonRepository.existsByNameIgnoreCaseAndIdNot(name, seasonId)) {
            throw new ConflictException("Season '" + name + "' already exists.");
        }
        season.update(name, request.startsOn(), request.endsOn(), request.maxLandHectares(), request.distributionDate());
        audit(AuditEventType.SEASON_UPDATED, actorUserId, seasonId, "Season '" + name + "' updated");
        return toResponse(season, null);
    }

    @Transactional(readOnly = true)
    public SeasonResponse findById(Long seasonId) {
        Season season = seasonRepository.findById(seasonId).orElseThrow(() -> notFound(seasonId));
        EnrolmentWindow open = windowRepository.findBySeasonIdAndStatus(seasonId, WindowStatus.OPEN).orElse(null);
        return toResponse(season, open);
    }

    @Transactional(readOnly = true)
    public PagedResponse<SeasonResponse> findAll(int page, int size, String sortBy, String direction) {
        Pageable pageable = Paging.of(page, size, sortBy, direction, SEASON_SORT_FIELDS);
        Page<Season> seasons = seasonRepository.findAll(pageable);

        // One extra query for the whole page, instead of one per season.
        List<Long> seasonIds = seasons.map(Season::getId).getContent();
        Map<Long, EnrolmentWindow> openBySeason = seasonIds.isEmpty()
                ? Map.of()
                : windowRepository.findOpenForSeasons(seasonIds).stream()
                        .collect(Collectors.toMap(w -> w.getSeason().getId(), Function.identity()));

        return PagedResponse.from(seasons.map(s -> toResponse(s, openBySeason.get(s.getId()))));
    }

    // ---------- enrolment windows ----------

    @Transactional
    public EnrolmentWindowResponse openWindow(Long seasonId, Long actorUserId) {
        // Lock the season row first: two registrars clicking "open" at once are handled one after the other.
        Season season = seasonRepository.findByIdForUpdate(seasonId).orElseThrow(() -> notFound(seasonId));
        if (season.hasEnded(LocalDate.now())) {
            throw new BusinessRuleException("This season ended on " + season.getEndsOn()
                    + "; an enrolment window can no longer be opened.");
        }
        if (windowRepository.findBySeasonIdAndStatus(seasonId, WindowStatus.OPEN).isPresent()) {
            throw new ConflictException("An enrolment window is already open for this season.");
        }

        AppUser actor = findActor(actorUserId);
        // saveAndFlush: write now, so the one-open-window index is checked inside this method.
        EnrolmentWindow window = windowRepository.saveAndFlush(new EnrolmentWindow(season, actor, Instant.now()));
        audit(AuditEventType.ENROLMENT_WINDOW_OPENED, actor, seasonId,
                "Enrolment window " + window.getId() + " opened for season '" + season.getName() + "'");
        return EnrolmentWindowResponse.from(window);
    }

    @Transactional
    public EnrolmentWindowResponse closeWindow(Long seasonId, Long actorUserId) {
        Season season = seasonRepository.findByIdForUpdate(seasonId).orElseThrow(() -> notFound(seasonId));
        EnrolmentWindow window = windowRepository.findBySeasonIdAndStatus(seasonId, WindowStatus.OPEN)
                .orElseThrow(() -> new ConflictException("No enrolment window is open for this season."));

        AppUser actor = findActor(actorUserId);
        window.close(actor, Instant.now());
        audit(AuditEventType.ENROLMENT_WINDOW_CLOSED, actor, seasonId,
                "Enrolment window " + window.getId() + " closed for season '" + season.getName() + "'");
        return EnrolmentWindowResponse.from(window);
    }

    @Transactional(readOnly = true)
    public PagedResponse<EnrolmentWindowResponse> windows(Long seasonId, int page, int size,
                                                         String sortBy, String direction) {
        if (!seasonRepository.existsById(seasonId)) {
            throw notFound(seasonId);
        }
        Pageable pageable = Paging.of(page, size, sortBy, direction, WINDOW_SORT_FIELDS);
        return PagedResponse.from(windowRepository.findBySeasonId(seasonId, pageable).map(EnrolmentWindowResponse::from));
    }

    /** Used by enrolment (Step 5c): farmers may enrol only while the season's window is open. */
    @Transactional(readOnly = true)
    public boolean isEnrolmentOpen(Long seasonId) {
        return windowRepository.findBySeasonIdAndStatus(seasonId, WindowStatus.OPEN).isPresent();
    }

    // ---------- helpers ----------

    private void validateDates(SeasonRequest request) {
        if (!request.endsOn().isAfter(request.startsOn())) {
            throw new BadRequestException("endsOn must be after startsOn.");
        }
        if (request.distributionDate().isBefore(request.startsOn())
                || request.distributionDate().isAfter(request.endsOn())) {
            throw new BadRequestException("distributionDate must fall within the season (startsOn to endsOn).");
        }
    }

    private SeasonResponse toResponse(Season season, EnrolmentWindow openWindow) {
        return new SeasonResponse(
                season.getId(),
                season.getName(),
                season.getStartsOn(),
                season.getEndsOn(),
                season.getMaxLandHectares(),
                season.getDistributionDate(),
                openWindow != null,
                openWindow == null ? null : openWindow.getId(),
                season.getCreatedAt(),
                season.getUpdatedAt());
    }

    private AppUser findActor(Long actorUserId) {
        return appUserRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private void audit(AuditEventType type, Long actorUserId, Long seasonId, String details) {
        audit(type, findActor(actorUserId), seasonId, details);
    }

    private void audit(AuditEventType type, AppUser actor, Long seasonId, String details) {
        auditLogService.recordInCurrentTransaction(type, true, actor.getId(), actor.getUsername(),
                "SEASON", seasonId, details, null);
    }

    private static ResourceNotFoundException notFound(Long seasonId) {
        return new ResourceNotFoundException("Season with id " + seasonId + " was not found.");
    }
}
