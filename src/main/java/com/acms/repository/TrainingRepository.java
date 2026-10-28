package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.Training;

public interface TrainingRepository extends BaseRepository<Training> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
