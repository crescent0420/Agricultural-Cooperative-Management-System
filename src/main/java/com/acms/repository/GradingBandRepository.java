package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.GradingBand;
import java.util.List;

public interface GradingBandRepository extends BaseRepository<GradingBand> {

    /** Highest band first, so the first band whose minimum is reached is the grade. */
    List<GradingBand> findAllByOrderByMinScoreDesc();
}
