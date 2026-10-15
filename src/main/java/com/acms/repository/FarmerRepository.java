
package com.acms.repository;

import com.acms.entity.Farmer;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FarmerRepository extends JpaRepository<Farmer, Long> {

    boolean existsByNationalId(String nationalId);

    @Query(value = "SELECT nextval('farmer_membership_seq')", nativeQuery = true)
    Long nextMembershipSequence();

    @Override
    @EntityGraph(attributePaths = {"cooperative", "cooperative.district"})
    Optional<Farmer> findById(Long id);
}
