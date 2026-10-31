package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.EnrolmentWindow;
import com.acms.enums.WindowStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrolmentWindowRepository extends BaseRepository<EnrolmentWindow> {

    Optional<EnrolmentWindow> findBySeasonIdAndStatus(Long seasonId, WindowStatus status);

    boolean existsBySeasonId(Long seasonId);

    @EntityGraph(attributePaths = {"season", "openedBy", "closedBy"})
    Page<EnrolmentWindow> findBySeasonId(Long seasonId, Pageable pageable);

    /** The open window of each of these seasons, in ONE query (avoids one query per season in a list). */
    @Query("SELECT w FROM EnrolmentWindow w WHERE w.status = com.acms.enums.WindowStatus.OPEN AND w.season.id IN :seasonIds")
    List<EnrolmentWindow> findOpenForSeasons(@Param("seasonIds") Collection<Long> seasonIds);
}