package com.acms.repository;
import com.acms.entity.FarmerTraining;
import com.acms.entity.Training;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface FarmerTrainingRepository extends JpaRepository<FarmerTraining, Long> {
    boolean existsByFarmerIdAndTrainingId(Long farmerId, Long trainingId);
    @EntityGraph(attributePaths = "training")
    List<FarmerTraining> findByFarmerId(Long farmerId, Sort sort);
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
