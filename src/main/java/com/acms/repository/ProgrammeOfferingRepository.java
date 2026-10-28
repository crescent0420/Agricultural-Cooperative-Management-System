package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.ProgrammeOffering;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgrammeOfferingRepository
        extends BaseRepository<ProgrammeOffering>, JpaSpecificationExecutor<ProgrammeOffering> {

    boolean existsByProgrammeIdAndSeasonId(Long programmeId, Long seasonId);

    /** One query that also loads the programme, its district, the season and the Field Officer. */
    @EntityGraph(attributePaths = {"programme", "programme.district", "season", "fieldOfficer"})
    Optional<ProgrammeOffering> findWithDetailsById(Long id);

    /**
     * SELECT ... FOR UPDATE: locks this offering's row until the transaction ends.
     * Changing the capacity here and enrolling both take this lock,
     * so they happen one after the other and can never overtake each other.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM ProgrammeOffering o WHERE o.id = :id")
    Optional<ProgrammeOffering> findByIdForUpdate(@Param("id") Long id);

    // ---------- capacity (Step 5c) ----------

    /**
     * 3. The offering's row, FOR UPDATE, read straight from the database (never a stale copy).
     *    Every enrolment of this offering queues here, one at a time, for the last few statements.
     */
    @Query(value = "SELECT hectares_per_participant FROM programme_offerings WHERE id = :id FOR UPDATE",
           nativeQuery = true)
    BigDecimal lockAndReadHectares(@Param("id") Long id);

    /**
     * Takes one place only if one is free, in a single statement. Returns 1 if a place was taken, 0 if full.
     * Even without the lock above, PostgreSQL re-checks the WHERE condition on the newest version of the row,
     * so two farmers can never both take the last place.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE ProgrammeOffering o SET o.enrolledCount = o.enrolledCount + 1
            WHERE o.id = :id AND o.enrolledCount < o.maxParticipants
            """)
    int takePlace(@Param("id") Long id);

    /** Gives one place back after a cancellation. Returns 1 on success. */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE ProgrammeOffering o SET o.enrolledCount = o.enrolledCount - 1
            WHERE o.id = :id AND o.enrolledCount > 0
            """)
    int releasePlace(@Param("id") Long id);
}
