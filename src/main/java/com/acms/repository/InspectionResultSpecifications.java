package com.acms.repository;

import com.acms.entity.InspectionResult;
import com.acms.entity.ResultStatus;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class InspectionResultSpecifications {

    private InspectionResultSpecifications() {
    }

    /** Every filter is optional (null = no filter). */
    public static Specification<InspectionResult> search(Long offeringId, Long farmerId, Long districtId,
                                                         Long fieldOfficerId, ResultStatus status) {
        return (root, query, cb) -> {
            Class<?> resultType = query.getResultType();
            if (resultType != Long.class && resultType != long.class) {
                Fetch<Object, Object> enrolment = root.fetch("enrolment", JoinType.INNER);
                enrolment.fetch("farmer", JoinType.INNER);
                Fetch<Object, Object> offering = enrolment.fetch("offering", JoinType.INNER);
                offering.fetch("programme", JoinType.INNER);
                offering.fetch("season", JoinType.INNER);
            }
            List<Predicate> predicates = new ArrayList<>();
            if (offeringId != null) {
                predicates.add(cb.equal(root.get("enrolment").get("offering").get("id"), offeringId));
            }
            if (farmerId != null) {
                predicates.add(cb.equal(root.get("enrolment").get("farmer").get("id"), farmerId));
            }
            if (districtId != null) {
                predicates.add(cb.equal(
                        root.get("enrolment").get("offering").get("programme").get("district").get("id"), districtId));
            }
            if (fieldOfficerId != null) {
                predicates.add(cb.equal(root.get("enrolment").get("offering").get("fieldOfficer").get("id"),
                        fieldOfficerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
