package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.ContributionObligation;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContributionObligationRepository
        extends BaseRepository<ContributionObligation>, JpaSpecificationExecutor<ContributionObligation> {

    boolean existsByFarmerIdAndSeasonId(Long farmerId, Long seasonId);

    /** Only the id, without loading the row (so it can be locked first and then read fresh). */
    @Query("SELECT o.id FROM ContributionObligation o WHERE o.farmer.id = :farmerId AND o.season.id = :seasonId")
    Optional<Long> findIdByFarmerAndSeason(@Param("farmerId") Long farmerId, @Param("seasonId") Long seasonId);

    @EntityGraph(attributePaths = {"farmer", "season"})
    Optional<ContributionObligation> findWithDetailsById(Long id);

    /**
     * FOR UPDATE: every change of money for ONE contribution (new payment, confirmation, adjustment)
     * runs one at a time, so the balance checks always see the latest numbers. Returns null if absent.
     */
    @Query(value = "SELECT id FROM contribution_obligations WHERE id = :id FOR UPDATE", nativeQuery = true)
    Long lockForUpdate(@Param("id") Long id);

    /**
     * FOR SHARE: used by enrolment while it checks the 30% rule. Many enrolments may share it, but a
     * change of money for this contribution waits until they finish (and the other way round).
     */
    @Query(value = "SELECT id FROM contribution_obligations WHERE id = :id FOR SHARE", nativeQuery = true)
    Long lockForShare(@Param("id") Long id);
}
