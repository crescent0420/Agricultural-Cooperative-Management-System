package com.acms.repository;

import com.acms.entity.Cooperative;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CooperativeRepository extends JpaRepository<Cooperative, Long> {

    boolean existsByDistrictIdAndNameIgnoreCase(Long districtId, String name);

    boolean existsByDistrictIdAndNameIgnoreCaseAndIdNot(
            Long districtId, String name, Long id);

    @Override
    @EntityGraph(attributePaths = "district")
    List<Cooperative> findAll(Sort sort);

    @EntityGraph(attributePaths = "district")
    List<Cooperative> findByDistrictId(Long districtId, Sort sort);
}
