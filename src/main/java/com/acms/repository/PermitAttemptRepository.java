package com.acms.repository;

import com.acms.common.BaseRepository;
import com.acms.entity.PermitAttempt;
import com.acms.enums.PermitOutcome;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PermitAttemptRepository extends BaseRepository<PermitAttempt>, JpaSpecificationExecutor<PermitAttempt> {

    /** Both filters optional (null = no filter). */
    static Specification<PermitAttempt> search(Long farmerId, Long enrolmentId, PermitOutcome outcome) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (farmerId != null) {
                predicates.add(cb.equal(root.get("farmerId"), farmerId));
            }
            if (enrolmentId != null) {
                predicates.add(cb.equal(root.get("enrolmentId"), enrolmentId));
            }
            if (outcome != null) {
                predicates.add(cb.equal(root.get("outcome"), outcome));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
