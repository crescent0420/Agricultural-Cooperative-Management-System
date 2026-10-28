package com.acms.repository;

import com.acms.entity.ContributionAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContributionAdjustmentRepository extends JpaRepository<ContributionAdjustment, Long> {

    @EntityGraph(attributePaths = "adjustedBy")
    Page<ContributionAdjustment> findByObligationId(Long obligationId, Pageable pageable);
}
