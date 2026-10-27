package com.acms.repository;

import com.acms.entity.ProgrammeOffering;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgrammeOfferingRepository
        extends JpaRepository<ProgrammeOffering, Long>, JpaSpecificationExecutor<ProgrammeOffering> {

    boolean existsByProgrammeIdAndSeasonId(Long programmeId, Long seasonId);

    /** One query that also loads the programme, its district, the season and the Field Officer. */
    @EntityGraph(attributePaths = {"programme", "programme.district", "season", "fieldOfficer"})
    Optional<ProgrammeOffering> findWithDetailsById(Long id);

    /**
     * SELECT ... FOR UPDATE: locks this offering's row until the transaction ends.
     * Changing the capacity here and enrolling in Step 5c both take this lock,
     * so they happen one after the other and can never overtake each other.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM ProgrammeOffering o WHERE o.id = :id")
    Optional<ProgrammeOffering> findByIdForUpdate(@Param("id") Long id);
}
