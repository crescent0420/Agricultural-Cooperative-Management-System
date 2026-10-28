package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.CropProgramme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

public interface CropProgrammeRepository extends BaseRepository<CropProgramme> {

    boolean existsByDistrictIdAndNameIgnoreCase(Long districtId, String name);

    boolean existsByDistrictIdAndNameIgnoreCaseAndIdNot(Long districtId, String name, Long id);

    @Override
    @EntityGraph(attributePaths = "district")
    Page<CropProgramme> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "district")
    Page<CropProgramme> findByDistrictId(Long districtId, Pageable pageable);
}