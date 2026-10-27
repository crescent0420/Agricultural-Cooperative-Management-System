package com.acms.repository;

import com.acms.entity.ProgrammeOffering;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ProgrammeOfferingSpecifications {

    private ProgrammeOfferingSpecifications() {
    }

    /** Every filter is optional: a null value means "do not filter on this". */
    public static Specification<ProgrammeOffering> search(Long seasonId, Long programmeId,
                                                          Long districtId, Long fieldOfficerId) {
        return (root, query, cb) -> {
            // Load the programme, its district, the season and the officer in the same query (no N+1),
            // but not in the separate "count" query that pagination runs.
            Class<?> resultType = query.getResultType();
            if (resultType != Long.class && resultType != long.class) {
                root.fetch("programme", JoinType.INNER).fetch("district", JoinType.INNER);
                root.fetch("season", JoinType.INNER);
                root.fetch("fieldOfficer", JoinType.INNER);
            }

            List<Predicate> predicates = new ArrayList<>();
            if (seasonId != null) {
                predicates.add(cb.equal(root.get("season").get("id"), seasonId));
            }
            if (programmeId != null) {
                predicates.add(cb.equal(root.get("programme").get("id"), programmeId));
            }
            if (districtId != null) {
                predicates.add(cb.equal(root.get("programme").get("district").get("id"), districtId));
            }
            if (fieldOfficerId != null) {
                predicates.add(cb.equal(root.get("fieldOfficer").get("id"), fieldOfficerId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
