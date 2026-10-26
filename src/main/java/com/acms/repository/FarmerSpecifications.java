package com.acms.repository;

import com.acms.entity.Farmer;
import com.acms.entity.FarmerStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class FarmerSpecifications {

    private FarmerSpecifications() {
    }

    /** districtId is the caller's forced district scope (null = no district restriction). */
    public static Specification<Farmer> search(String name, String membershipNumber, Long cooperativeId,
                                               FarmerStatus status, Long districtId) {
        return (root, query, cb) -> {
            Class<?> resultType = query.getResultType();
            if (resultType != Long.class && resultType != long.class) {
                root.fetch("cooperative", JoinType.INNER).fetch("district", JoinType.INNER);
            }

            List<Predicate> predicates = new ArrayList<>();
            if (hasText(name)) {
                predicates.add(cb.like(cb.lower(root.get("fullName")), contains(name), '\\'));
            }
            if (hasText(membershipNumber)) {
                predicates.add(cb.like(cb.lower(root.get("membershipNumber")), contains(membershipNumber), '\\'));
            }
            if (cooperativeId != null) {
                predicates.add(cb.equal(root.get("cooperative").get("id"), cooperativeId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (districtId != null) {
                predicates.add(cb.equal(root.get("cooperative").get("district").get("id"), districtId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** Builds a "contains" pattern and neutralises the LIKE wildcards % and _ typed by the user. */
    private static String contains(String input) {
        String escaped = input.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}