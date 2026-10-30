package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.InspectionResult;
import com.acms.enums.ResultStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InspectionResultRepository
        extends BaseRepository<InspectionResult>, JpaSpecificationExecutor<InspectionResult> {

    /** Every change of a result locks its row first: a Field Officer and an Agronomist can never overlap. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM InspectionResult r WHERE r.id = :id")
    Optional<InspectionResult> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM InspectionResult r WHERE r.enrolment.id = :enrolmentId")
    Optional<InspectionResult> findByEnrolmentIdForUpdate(@Param("enrolmentId") Long enrolmentId);

    @EntityGraph(attributePaths = {"enrolment", "enrolment.farmer", "enrolment.offering",
                                   "enrolment.offering.programme", "enrolment.offering.programme.district",
                                   "enrolment.offering.season", "enrolment.offering.fieldOfficer"})
    Optional<InspectionResult> findWithDetailsById(Long id);

    @Query("SELECT r.enrolment.offering.id FROM InspectionResult r WHERE r.id = :id")
    Optional<Long> findOfferingIdById(@Param("id") Long id);

    /** Inspections are frozen once any result of the offering has left the DRAFT stage. */
    boolean existsByEnrolmentOfferingIdAndStatusIn(Long offeringId, Collection<ResultStatus> statuses);
}
