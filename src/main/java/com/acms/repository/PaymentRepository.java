package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.Payment;
import com.acms.entity.PaymentStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends BaseRepository<Payment> {

    Optional<Payment> findByTransactionReference(String transactionReference);

    Page<Payment> findByObligationId(Long obligationId, Pageable pageable);

    /** Reads only the contribution id, without loading the payment (so it is loaded fresh after the lock). */
    @Query("SELECT p.obligation.id FROM Payment p WHERE p.id = :id")
    Optional<Long> findObligationIdById(@Param("id") Long id);

    /** Total of one contribution's payments in one status (null when there are none). */
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.obligation.id = :obligationId AND p.status = :status")
    BigDecimal sumAmount(@Param("obligationId") Long obligationId, @Param("status") PaymentStatus status);

    /** The same totals for a whole page of contributions in one query: rows of [obligationId, status, sum]. */
    @Query("""
            SELECT p.obligation.id, p.status, SUM(p.amount) FROM Payment p
            WHERE p.obligation.id IN :obligationIds
            GROUP BY p.obligation.id, p.status
            """)
    List<Object[]> sumAmountsByStatus(@Param("obligationIds") Collection<Long> obligationIds);
}
