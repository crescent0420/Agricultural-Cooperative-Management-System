package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.Enrolment;
import com.acms.enums.EnrolmentStatus;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrolmentRepository extends BaseRepository<Enrolment>, JpaSpecificationExecutor<Enrolment> {

    boolean existsByOfferingIdAndFarmerIdAndStatus(Long offeringId, Long farmerId, EnrolmentStatus status);

    /** Used to freeze a programme's prerequisites while farmers are enrolled in it. */
    boolean existsByOfferingProgrammeIdAndStatus(Long programmeId, EnrolmentStatus status);

    /** Total land of this farmer's active enrolments in one season (null when there are none). */
    @Query("""
            SELECT SUM(e.hectares) FROM Enrolment e
            WHERE e.farmer.id = :farmerId AND e.offering.season.id = :seasonId AND e.status = :status
            """)
    BigDecimal sumHectares(@Param("farmerId") Long farmerId, @Param("seasonId") Long seasonId,
                           @Param("status") EnrolmentStatus status);

    /** Reads only the farmer id, without loading the enrolment (so it can be locked fresh afterwards). */
    @Query("SELECT e.farmer.id FROM Enrolment e WHERE e.id = :id")
    Optional<Long> findFarmerIdById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Enrolment e WHERE e.id = :id")
    Optional<Enrolment> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"offering", "offering.programme", "offering.programme.district",
                                   "offering.season", "offering.fieldOfficer", "farmer"})
    Optional<Enrolment> findWithDetailsById(Long id);

    // ---------- row locks used by enrolment and cancellation, always taken in this order ----------

    /**
     * 1. The farmer's row, FOR UPDATE: one farmer's enrolments and cancellations run one at a time,
     *    so "already enrolled" and the season land limit are checked on up-to-date data.
     */
    @Query(value = "SELECT id FROM farmers WHERE id = :farmerId FOR UPDATE", nativeQuery = true)
    Long lockFarmer(@Param("farmerId") Long farmerId);

    /**
     * 2. The season's row, FOR SHARE: thousands of enrolments may hold it together, but closing the
     *    window (which takes FOR UPDATE) waits until they finish, and enrolments that start later
     *    wait for the close and then see the window as closed.
     */
    @Query(value = "SELECT id FROM seasons WHERE id = :seasonId FOR SHARE", nativeQuery = true)
    Long lockSeasonForShare(@Param("seasonId") Long seasonId);
}
