package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.Inspection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InspectionRepository extends BaseRepository<Inspection> {

    List<Inspection> findByOfferingIdOrderByIdAsc(Long offeringId);

    boolean existsByOfferingIdAndNameIgnoreCase(Long offeringId, String name);

    boolean existsByOfferingIdAndNameIgnoreCaseAndIdNot(Long offeringId, String name, Long id);

    /** Reads only the offering id, without loading the inspection (so it is read fresh after the lock). */
    @Query("SELECT i.offering.id FROM Inspection i WHERE i.id = :id")
    java.util.Optional<Long> findOfferingIdById(@Param("id") Long id);

    /**
     * Changing an offering's inspections takes the offering's row FOR UPDATE; entering and submitting
     * scores take it FOR SHARE. So the inspections cannot change while scores are being checked.
     */
    @Query(value = "SELECT id FROM programme_offerings WHERE id = :id FOR UPDATE", nativeQuery = true)
    Long lockOfferingForUpdate(@Param("id") Long offeringId);

    @Query(value = "SELECT id FROM programme_offerings WHERE id = :id FOR SHARE", nativeQuery = true)
    Long lockOfferingForShare(@Param("id") Long offeringId);
}
