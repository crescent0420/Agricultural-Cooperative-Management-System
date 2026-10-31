package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.ResultCorrection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

public interface ResultCorrectionRepository extends BaseRepository<ResultCorrection> {

    /** Oldest first: applying them in order gives the current values. */
    @EntityGraph(attributePaths = {"inspection", "correctedBy"})
    List<ResultCorrection> findByResultIdOrderByIdAsc(Long resultId);
}
