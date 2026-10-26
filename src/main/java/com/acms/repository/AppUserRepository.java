package com.acms.repository;

import com.acms.entity.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<AppUser> findByFarmerId(Long farmerId);

    boolean existsByFarmerId(Long farmerId);

    /**
     * Takes a PostgreSQL "advisory lock" that is released automatically when the transaction ends.
     * Only one registration can hold it at a time, so two people registering on an empty database
     * at the same moment cannot both become the first System Administrator.
     */
    @Query(value = "SELECT 1 FROM (SELECT pg_advisory_xact_lock(727001)) AS registration_lock", nativeQuery = true)
    Integer lockRegistrations();
}
