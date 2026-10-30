package com.acms.service;

import com.acms.common.BaseService;
import com.acms.common.PagedResponse;
import com.acms.common.Paging;
import com.acms.dto.PaymentRequest;
import com.acms.dto.PaymentResponse;
import com.acms.entity.AppUser;
import com.acms.entity.ContributionObligation;
import com.acms.entity.Payment;
import com.acms.enums.AuditEventType;
import com.acms.enums.PaymentStatus;
import com.acms.exception.BusinessRuleException;
import com.acms.exception.ConflictException;
import com.acms.exception.ResourceNotFoundException;
import com.acms.repository.ContributionObligationRepository;
import com.acms.repository.PaymentRepository;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payments: requested (PENDING), then CONFIRMED when the operator or bank confirms the money arrived,
 * or FAILED. Only confirmed payments reduce the outstanding balance.
 *
 * Every change of money for one contribution first locks that contribution's row, so payment requests,
 * confirmations and adjustments of the same contribution happen one after the other.
 */
@Service
@RequiredArgsConstructor
public class PaymentService extends BaseService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "requestedAt", "amount", "status");

    private final PaymentRepository paymentRepository;
    private final ContributionObligationRepository obligationRepository;
    private final ContributionService contributionService;
    private final FarmerAccessService farmerAccessService;
    private final AuditLogService auditLogService;

    /** The result of a payment request: created = false when it was a repeat of an earlier request. */
    public record PaymentResult(PaymentResponse payment, boolean created) {
    }

    /**
     * Idempotent: the client sends a transactionReference it chose for this payment.
     *  - first time:                          a new PENDING payment              -> 201
     *  - same reference, same details again:  the ORIGINAL payment, nothing new  -> 200
     *  - same reference, different details:   refused                            -> 409
     * Two identical requests arriving at the same moment are made to queue on the contribution's row lock,
     * so the second one finds the first; the unique constraint on transaction_reference is the final guard.
     */
    @Transactional
    public PaymentResult request(PaymentRequest request) {
        String reference = request.transactionReference().trim();
        Long obligationId = request.contributionId();

        if (obligationRepository.lockForUpdate(obligationId) == null) {
            throw ContributionService.notFound(obligationId);
        }
        AppUser actor = farmerAccessService.getCurrentUser();

        Optional<Payment> existing = paymentRepository.findByTransactionReference(reference);
        if (existing.isPresent()) {
            Payment original = existing.get();
            if (!original.isSameRequest(obligationId, request.amount(), request.method())) {
                throw new ConflictException("transactionReference '" + reference
                        + "' was already used for a different payment.");
            }
            audit(AuditEventType.PAYMENT_REQUEST_REPEATED, actor, original.getId(),
                    "Repeated request " + reference + " answered with existing payment " + original.getId());
            return new PaymentResult(PaymentResponse.from(original), false);
        }

        ContributionObligation obligation = obligationRepository.findById(obligationId)
                .orElseThrow(() -> ContributionService.notFound(obligationId));
        ContributionBalance balance = contributionService.balanceOf(obligation);
        if (request.amount().compareTo(balance.requestable()) > 0) {
            throw new BusinessRuleException("amount " + request.amount() + " is more than can still be paid: "
                    + balance.outstanding() + " outstanding, of which " + balance.pending()
                    + " is already waiting for confirmation.");
        }

        Payment payment = paymentRepository.saveAndFlush(
                new Payment(obligation, reference, request.amount(), request.method(), actor));
        audit(AuditEventType.PAYMENT_REQUESTED, actor, payment.getId(),
                "Payment " + reference + " of " + request.amount() + " by " + request.method()
                        + " for contribution " + obligationId);
        return new PaymentResult(PaymentResponse.from(payment), true);
    }

    /**
     * Records the confirmation from the operator or bank. Confirming again with the SAME provider reference
     * (operators often resend confirmations) returns the payment unchanged; anything else is refused.
     */
    @Transactional
    public PaymentResponse confirm(Long paymentId, String providerReference) {
        Payment payment = lockContributionAndLoad(paymentId);
        String reference = providerReference.trim();

        if (payment.getStatus() == PaymentStatus.CONFIRMED) {
            if (reference.equals(payment.getProviderReference())) {
                return PaymentResponse.from(payment);
            }
            throw new ConflictException("Payment " + paymentId + " is already confirmed with another provider reference.");
        }
        if (payment.getStatus() == PaymentStatus.FAILED) {
            throw new ConflictException("Payment " + paymentId + " has failed and can no longer be confirmed.");
        }

        // The amount owed may have been lowered since the request: never confirm more than is owed.
        ContributionBalance balance = contributionService.balanceOf(payment.getObligation());
        if (payment.getAmount().compareTo(balance.outstanding()) > 0) {
            throw new BusinessRuleException("Confirming " + payment.getAmount() + " would pay more than the "
                    + balance.outstanding() + " outstanding.");
        }

        AppUser actor = farmerAccessService.getCurrentUser();
        payment.confirm(actor, reference);
        paymentRepository.flush();   // one provider reference can confirm only one payment (unique index)
        audit(AuditEventType.PAYMENT_CONFIRMED, actor, paymentId,
                "Payment " + payment.getTransactionReference() + " confirmed, provider reference " + reference);
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse fail(Long paymentId, String reason) {
        Payment payment = lockContributionAndLoad(paymentId);
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ConflictException("Payment " + paymentId + " is " + payment.getStatus()
                    + "; only a PENDING payment can fail.");
        }
        AppUser actor = farmerAccessService.getCurrentUser();
        String cleanReason = reason.trim();
        payment.fail(actor, cleanReason);
        paymentRepository.flush();
        audit(AuditEventType.PAYMENT_FAILED, actor, paymentId,
                "Payment " + payment.getTransactionReference() + " failed: " + cleanReason);
        return PaymentResponse.from(payment);
    }

    // ---------- reading ----------

    @Transactional(readOnly = true)
    public PaymentResponse findById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> notFound(paymentId));
        contributionService.requireCanView(payment.getObligation());
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> findForContribution(Long obligationId, int page, int size,
                                                             String sortBy, String direction) {
        Pageable pageable = Paging.of(page, size, sortBy, direction, SORTABLE_FIELDS);
        ContributionObligation obligation = obligationRepository.findWithDetailsById(obligationId)
                .orElseThrow(() -> ContributionService.notFound(obligationId));
        contributionService.requireCanView(obligation);
        return PagedResponse.from(paymentRepository.findByObligationId(obligationId, pageable)
                .map(PaymentResponse::from));
    }

    // ---------- helpers ----------

    /** Lock order: contribution first, then read the payment fresh from the database. */
    private Payment lockContributionAndLoad(Long paymentId) {
        Long obligationId = paymentRepository.findObligationIdById(paymentId)
                .orElseThrow(() -> notFound(paymentId));
        obligationRepository.lockForUpdate(obligationId);
        return paymentRepository.findById(paymentId).orElseThrow(() -> notFound(paymentId));
    }

    private void audit(AuditEventType type, AppUser actor, Long paymentId, String details) {
        auditLogService.recordInCurrentTransaction(type, true, actor.getId(), actor.getUsername(),
                "PAYMENT", paymentId, details, null);
    }

    private static ResourceNotFoundException notFound(Long paymentId) {
        return new ResourceNotFoundException("Payment with id " + paymentId + " was not found.");
    }
}
