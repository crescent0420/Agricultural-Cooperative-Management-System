package com.acms.repository;

import com.acms.entity.Cooperative;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CooperativeRepository extends JpaRepository<Cooperative, Long> {

    boolean existsByDistrictIdAndNameIgnoreCase(Long districtId, String name);

    boolean existsByDistrictIdAndNameIgnoreCaseAndIdNot(Long districtId, String name, Long id);

    @Override
    @EntityGraph(attributePaths = "district")
    Page<Cooperative> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "district")
    Page<Cooperative> findByDistrictId(Long districtId, Pageable pageable);
}