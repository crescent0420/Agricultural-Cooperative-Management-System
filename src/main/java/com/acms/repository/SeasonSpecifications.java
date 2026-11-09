package com.acms.repository;

import com.acms.entity.EnrolmentWindow;
import com.acms.entity.Season;
import com.acms.enums.WindowStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** The optional filters of GET /api/seasons. */
public final class SeasonSpecifications {

    private SeasonSpecifications() {
    }

    /**
     * @param name          part of the season name, any letter case (null or blank = no filter)
     * @param enrolmentOpen true = only seasons with an OPEN enrolment window, false = only those without one,
     *                      null = no filter
     */
    public static Specification<Season> search(String name, Boolean enrolmentOpen) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (FarmerSpecifications.hasText(name)) {
                predicates.add(cb.like(cb.lower(root.get("name")), FarmerSpecifications.contains(name), '\\'));
            }
            if (enrolmentOpen != null) {
                // EXISTS (an OPEN window of this season): at most one can be open, enforced by the database.
                Subquery<Long> open = query.subquery(Long.class);
                Root<EnrolmentWindow> window = open.from(EnrolmentWindow.class);
                open.select(window.<Long>get("id"))
                        .where(cb.equal(window.get("season"), root),
                                cb.equal(window.get("status"), WindowStatus.OPEN));
                predicates.add(enrolmentOpen ? cb.exists(open) : cb.not(cb.exists(open)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
