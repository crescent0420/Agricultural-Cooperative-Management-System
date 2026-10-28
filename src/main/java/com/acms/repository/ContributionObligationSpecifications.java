package com.acms.repository;

import com.acms.entity.ContributionObligation;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ContributionObligationSpecifications {

    private ContributionObligationSpecifications() {
    }

    public static Specification<ContributionObligation> search(Long farmerId, Long seasonId) {
        return (root, query, cb) -> {
            Class<?> resultType = query.getResultType();
            if (resultType != Long.class && resultType != long.class) {
                root.fetch("farmer", JoinType.INNER);
                root.fetch("season", JoinType.INNER);
            }
            List<Predicate> predicates = new ArrayList<>();
            if (farmerId != null) {
                predicates.add(cb.equal(root.get("farmer").get("id"), farmerId));
            }
            if (seasonId != null) {
                predicates.add(cb.equal(root.get("season").get("id"), seasonId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
