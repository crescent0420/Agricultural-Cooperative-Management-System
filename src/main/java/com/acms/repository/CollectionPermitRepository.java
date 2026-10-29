package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.CollectionPermit;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

public interface CollectionPermitRepository extends BaseRepository<CollectionPermit> {

    Optional<CollectionPermit> findByEnrolmentId(Long enrolmentId);

    boolean existsByPermitNumber(String permitNumber);

    /** Everything the storekeeper's verification shows, in one query. */
    @EntityGraph(attributePaths = {"farmer", "enrolment", "enrolment.offering", "enrolment.offering.programme",
                                   "enrolment.offering.season"})
    Optional<CollectionPermit> findWithDetailsByPermitNumber(String permitNumber);

    @EntityGraph(attributePaths = {"farmer", "enrolment", "enrolment.offering", "enrolment.offering.programme",
                                   "enrolment.offering.season"})
    Page<CollectionPermit> findByFarmerId(Long farmerId, Pageable pageable);
}
