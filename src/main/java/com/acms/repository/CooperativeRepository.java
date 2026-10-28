package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.Cooperative;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

public interface CooperativeRepository extends BaseRepository<Cooperative> {

    boolean existsByDistrictIdAndNameIgnoreCase(Long districtId, String name);

    boolean existsByDistrictIdAndNameIgnoreCaseAndIdNot(Long districtId, String name, Long id);

    @Override
    @EntityGraph(attributePaths = "district")
    Page<Cooperative> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "district")
    Page<Cooperative> findByDistrictId(Long districtId, Pageable pageable);
}