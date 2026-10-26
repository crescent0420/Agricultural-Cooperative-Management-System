package com.acms.repository;

import com.acms.entity.CropProgramme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CropProgrammeRepository extends JpaRepository<CropProgramme, Long> {

    boolean existsByDistrictIdAndNameIgnoreCase(Long districtId, String name);

    boolean existsByDistrictIdAndNameIgnoreCaseAndIdNot(Long districtId, String name, Long id);

    @Override
    @EntityGraph(attributePaths = "district")
    Page<CropProgramme> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "district")
    Page<CropProgramme> findByDistrictId(Long districtId, Pageable pageable);
}