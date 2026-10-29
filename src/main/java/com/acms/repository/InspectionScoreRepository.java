package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.InspectionScore;
import java.util.List;

public interface InspectionScoreRepository extends BaseRepository<InspectionScore> {

    List<InspectionScore> findByResultId(Long resultId);

    boolean existsByInspectionId(Long inspectionId);
}
