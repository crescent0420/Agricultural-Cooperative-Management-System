package com.acms.repository;

import com.acms.entity.CropProgramme;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CropProgrammeRepository
        extends JpaRepository<CropProgramme, Long> {

    boolean existsByDistrictIdAndNameIgnoreCase(
            Long districtId, String name);

    boolean existsByDistrictIdAndNameIgnoreCaseAndIdNot(
            Long districtId, String name, Long id);

    @Override
    @EntityGraph(attributePaths = {"district", "prerequisites"})
    List<CropProgramme> findAll(Sort sort);

    @EntityGraph(attributePaths = {"district", "prerequisites"})
    List<CropProgramme> findByDistrictId(Long districtId, Sort sort);
}
