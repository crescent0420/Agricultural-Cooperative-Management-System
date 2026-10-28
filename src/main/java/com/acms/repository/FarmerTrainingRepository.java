package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.FarmerTraining;
import com.acms.entity.Training;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FarmerTrainingRepository
        extends BaseRepository<FarmerTraining> {

    boolean existsByFarmerIdAndTrainingId(Long farmerId, Long trainingId);

    @EntityGraph(attributePaths = "training")
    Page<FarmerTraining> findByFarmerId(
            Long farmerId, Pageable pageable);

    /**
     * Returns prerequisite trainings that the farmer has not completed.
     */
    @Query("""
            SELECT t
            FROM CropProgramme p JOIN p.prerequisites t
            WHERE p.id = :programmeId
              AND t.id NOT IN (
                    SELECT ft.training.id
                    FROM FarmerTraining ft
                    WHERE ft.farmer.id = :farmerId
              )
            ORDER BY t.name
            """)
    List<Training> findMissingPrerequisites(
            @Param("farmerId") Long farmerId,
            @Param("programmeId") Long programmeId
    );
}