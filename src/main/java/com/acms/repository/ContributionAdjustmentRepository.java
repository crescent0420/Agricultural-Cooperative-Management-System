package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.ContributionAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

public interface ContributionAdjustmentRepository extends BaseRepository<ContributionAdjustment> {

    @EntityGraph(attributePaths = "adjustedBy")
    Page<ContributionAdjustment> findByObligationId(Long obligationId, Pageable pageable);
}
