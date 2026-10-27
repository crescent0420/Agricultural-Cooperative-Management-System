package com.acms.repository;

import com.acms.entity.District;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictRepository extends JpaRepository<District, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    // The province is loaded in the same query (no extra query per district).
    @Override
    @EntityGraph(attributePaths = "province")
    Page<District> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "province")
    Page<District> findByProvinceId(Long provinceId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "province")
    Optional<District> findById(Long id);
}
