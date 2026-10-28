package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.Season;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeasonRepository extends BaseRepository<Season> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * SELECT ... FOR UPDATE: locks this season's row until the transaction ends.
     * Opening and closing windows for the same season therefore happen one at a time.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Season s WHERE s.id = :id")
    Optional<Season> findByIdForUpdate(@Param("id") Long id);
}