package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.Province;
import java.util.Optional;

public interface ProvinceRepository extends BaseRepository<Province> {

    /** Province names are unique whatever their case (uq_provinces_name_lower). */
    Optional<Province> findByNameIgnoreCase(String name);
}
