package com.acms.service;

import com.acms.common.BaseService;
import com.acms.common.PagedResponse;
import com.acms.common.Paging;
import com.acms.dto.ContributionAdjustmentRequest;
import com.acms.dto.ContributionAdjustmentResponse;
import com.acms.dto.ContributionRequest;
import com.acms.dto.ContributionResponse;
import com.acms.entity.AppUser;
import com.acms.entity.AuditEventType;
import com.acms.entity.ContributionAdjustment;
import com.acms.entity.ContributionObligation;
import com.acms.entity.Farmer;
import com.acms.entity.PaymentStatus;
import com.acms.entity.Role;
import com.acms.entity.Season;
import com.acms.exception.BadRequestException;
import com.acms.exception.BusinessRuleException;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.ContributionAdjustmentRepository;
import com.acms.repository.ContributionObligationRepository;
import com.acms.repository.ContributionObligationSpecifications;
import com.acms.repository.FarmerRepository;
import com.acms.repository.PaymentRepository;
import com.acms.repository.SeasonRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Input contributions. Finance Officers create them and change amounts (only through an adjustment,
 * which keeps the old value, the new value, the reason, who and when). Balances are always calculated
 * from the payments, never stored.
 */
@Service
@RequiredArgsConstructor
public class ContributionService extends BaseService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "createdAt", "amountDue");
    private static final Set<String> ADJUSTMENT_SORT_FIELDS = Set.of("id", "adjustedAt");
    private static final String NO_PERMISSION = "You do not have permission to perform this action.";

    private final ContributionObligationRepository obligationRepository;
    private final ContributionAdjustmentRepository adjustmentRepository;
    private final PaymentRepository paymentRepository;
    private final FarmerRepository farmerRepository;
    private final SeasonRepository seasonRepository;
    private final FarmerAccessService farmerAccessService;
    private final AuditLogService auditLogService;

    // ---------- changes (Finance Officer) ----------

    @Transactional
    public ContributionResponse create(ContributionRequest request) {
        Farmer farmer = farmerRepository.findById(request.farmerId())
                .orElseThrow(() -> new ResourceNotFoundException("Farmer with id " + request.farmerId() + " was not found."));
        Season season = seasonRepository.findById(request.seasonId())
                .orElseThrow(() -> new ResourceNotFoundException("Season with id " + request.seasonId() + " was not found."));
        if (obligationRepository.existsByFarmerIdAndSeasonId(farmer.getId(), season.getId())) {
            throw new ConflictException("Farmer " + farmer.getMembershipNumber()
                    + " already has a contribution for season '" + season.getName() + "'.");
        }
        AppUser actor = farmerAccessService.getCurrentUser();
        // saveAndFlush: the unique (farmer, season) constraint is checked here, even if two requests race.
        ContributionObligation obligation = obligationRepository.saveAndFlush(
                new ContributionObligation(farmer, season, request.amountDue(), actor));
        audit(AuditEventType.CONTRIBUTION_CREATED, actor, obligation.getId(),
                "Contribution of " + request.amountDue() + " for farmer " + farmer.getMembershipNumber()
                        + ", season '" + season.getName() + "'");
        return ContributionResponse.from(obligation, ContributionBalance.of(obligation.getAmountDue(), null, null));
    }

    /** The amount owed may change, but never below what has already been confirmed as paid. */
    @Transactional
    public ContributionResponse adjust(Long obligationId, ContributionAdjustmentRequest request) {
        if (obligationRepository.lockForUpdate(obligationId) == null) {
            throw notFound(obligationId);
        }
        ContributionObligation obligation = obligationRepository.findWithDetailsById(obligationId)
                .orElseThrow(() -> notFound(obligationId));
        BigDecimal oldAmount = obligation.getAmountDue();
        BigDecimal newAmount = request.newAmountDue();
        if (newAmount.compareTo(oldAmount) == 0) {
            throw new BadRequestException("newAmountDue is the same as the current amount (" + oldAmount + ").");
        }
        ContributionBalance balance = balanceOf(obligation);
        if (newAmount.compareTo(balance.confirmedPaid()) < 0) {
            throw new BusinessRuleException("newAmountDue (" + newAmount + ") is less than the "
                    + balance.confirmedPaid() + " already paid and confirmed.");
        }

        AppUser actor = farmerAccessService.getCurrentUser();
        String reason = request.reason().trim();
        adjustmentRepository.save(new ContributionAdjustment(obligation, oldAmount, newAmount, reason, actor));
        obligation.changeAmountDue(newAmount);
        obligationRepository.flush();
        audit(AuditEventType.CONTRIBUTION_ADJUSTED, actor, obligationId,
                "Amount due changed from " + oldAmount + " to " + newAmount + ": " + reason);
        return ContributionResponse.from(obligation, balanceOf(obligation));
    }

    // ---------- reading ----------

    @Transactional(readOnly = true)
    public ContributionResponse findById(Long obligationId) {
        ContributionObligation obligation = obligationRepository.findWithDetailsById(obligationId)
                .orElseThrow(() -> notFound(obligationId));
        requireCanView(obligation);
        return ContributionResponse.from(obligation, balanceOf(obligation));
    }

    /** Finance Officer and Registrar: every contribution, filtered by farmer and/or season. */
    @Transactional(readOnly = true)
    public PagedResponse<ContributionResponse> findAll(Long farmerId, Long seasonId,
                                                      int page, int size, String sortBy, String direction) {
        return search(farmerId, seasonId, Paging.of(page, size, sortBy, direction, SORTABLE_FIELDS));
    }

    /** A farmer's own contributions and balances. */
    @Transactional(readOnly = true)
    public PagedResponse<ContributionResponse> findMine(Long seasonId,
                                                       int page, int size, String sortBy, String direction) {
        Pageable pageable = Paging.of(page, size, sortBy, direction, SORTABLE_FIELDS);
        Farmer farmer = farmerAccessService.requireCurrentFarmer();
        return search(farmer.getId(), seasonId, pageable);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ContributionAdjustmentResponse> adjustments(Long obligationId, int page, int size,
                                                                    String sortBy, String direction) {
        if (!obligationRepository.existsById(obligationId)) {
            throw notFound(obligationId);
        }
        Pageable pageable = Paging.of(page, size, sortBy, direction, ADJUSTMENT_SORT_FIELDS);
        return PagedResponse.from(adjustmentRepository.findByObligationId(obligationId, pageable)
                .map(ContributionAdjustmentResponse::from));
    }

    // ---------- shared with PaymentService and ContributionPolicy ----------

    /** The balance of one contribution, from its confirmed and pending payments. */
    @Transactional(readOnly = true)
    public ContributionBalance balanceOf(ContributionObligation obligation) {
        return ContributionBalance.of(
                obligation.getAmountDue(),
                paymentRepository.sumAmount(obligation.getId(), PaymentStatus.CONFIRMED),
                paymentRepository.sumAmount(obligation.getId(), PaymentStatus.PENDING));
    }

    /** Finance Officer and Registrar see every contribution; a farmer only their own (others: 403). */
    public void requireCanView(ContributionObligation obligation) {
        AppUser user = farmerAccessService.getCurrentUser();
        boolean allowed = switch (user.getRole()) {
            case FINANCE_OFFICER, UNION_REGISTRAR -> true;
            case FARMER -> user.getFarmer() != null
                    && user.getFarmer().getId().equals(obligation.getFarmer().getId());
            default -> false;
        };
        if (!allowed) {
            throw new AccessDeniedException(NO_PERMISSION);
        }
    }


    // ---------- helpers ----------

    private PagedResponse<ContributionResponse> search(Long farmerId, Long seasonId, Pageable pageable) {
        Page<ContributionObligation> obligations = obligationRepository.findAll(
                ContributionObligationSpecifications.search(farmerId, seasonId), pageable);

        // One query for the payment totals of the whole page (not one per contribution).
        List<Long> ids = obligations.map(ContributionObligation::getId).getContent();
        Map<Long, BigDecimal> confirmed = new HashMap<>();
        Map<Long, BigDecimal> pending = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : paymentRepository.sumAmountsByStatus(ids)) {
                Long obligationId = (Long) row[0];
                PaymentStatus status = (PaymentStatus) row[1];
                BigDecimal sum = (BigDecimal) row[2];
                if (status == PaymentStatus.CONFIRMED) {
                    confirmed.put(obligationId, sum);
                } else if (status == PaymentStatus.PENDING) {
                    pending.put(obligationId, sum);
                }
            }
        }
        return PagedResponse.from(obligations.map(o -> ContributionResponse.from(o,
                ContributionBalance.of(o.getAmountDue(), confirmed.get(o.getId()), pending.get(o.getId())))));
    }

    private void audit(AuditEventType type, AppUser actor, Long obligationId, String details) {
        auditLogService.recordInCurrentTransaction(type, true, actor.getId(), actor.getUsername(),
                "CONTRIBUTION", obligationId, details, null);
    }

    static ResourceNotFoundException notFound(Long obligationId) {
        return new ResourceNotFoundException("Contribution with id " + obligationId + " was not found.");
    }
}
