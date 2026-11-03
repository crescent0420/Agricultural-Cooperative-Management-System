package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import com.acms.enums.Role;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

public interface AppUserRepository extends BaseRepository<AppUser> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<AppUser> findByFarmerId(Long farmerId);

    boolean existsByFarmerId(Long farmerId);

        /** Enabled users of one role, by username; the district is loaded in the same query (no N+1). */
    @EntityGraph(attributePaths = "district")
    List<AppUser> findByRoleAndEnabledTrueOrderByUsernameAsc(Role role);

    /** The same, limited to one district. */
    @EntityGraph(attributePaths = "district")
    List<AppUser> findByRoleAndEnabledTrueAndDistrictIdOrderByUsernameAsc(Role role, Long districtId);

    /**
     * Takes a PostgreSQL "advisory lock" that is released automatically when the transaction ends.
     * Only one registration can hold it at a time, so two people registering on an empty database
     * at the same moment cannot both become the first System Administrator.
     */
    @Query(value = "SELECT 1 FROM (SELECT pg_advisory_xact_lock(727001)) AS registration_lock", nativeQuery = true)
    Integer lockRegistrations();
}
