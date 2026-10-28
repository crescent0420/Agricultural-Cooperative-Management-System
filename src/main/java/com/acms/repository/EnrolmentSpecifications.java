package com.acms.repository;

import com.acms.entity.Enrolment;
import com.acms.entity.EnrolmentStatus;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class EnrolmentSpecifications {

    private EnrolmentSpecifications() {
    }

    /** Every filter is optional: null means "do not filter on this". */
    public static Specification<Enrolment> search(Long farmerId, Long offeringId, Long seasonId,
                                                  EnrolmentStatus status) {
        return (root, query, cb) -> {
            // Load what the response shows in the same query, but not in pagination's count query.
            Class<?> resultType = query.getResultType();
            if (resultType != Long.class && resultType != long.class) {
                Fetch<Object, Object> offering = root.fetch("offering", JoinType.INNER);
                offering.fetch("programme", JoinType.INNER);
                offering.fetch("season", JoinType.INNER);
                root.fetch("farmer", JoinType.INNER);
            }

            List<Predicate> predicates = new ArrayList<>();
            if (farmerId != null) {
                predicates.add(cb.equal(root.get("farmer").get("id"), farmerId));
            }
            if (offeringId != null) {
                predicates.add(cb.equal(root.get("offering").get("id"), offeringId));
            }
            if (seasonId != null) {
                predicates.add(cb.equal(root.get("offering").get("season").get("id"), seasonId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
