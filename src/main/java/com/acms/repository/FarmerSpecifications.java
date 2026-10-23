package com.acms.repository;

import com.acms.entity.Farmer;
import com.acms.entity.FarmerStatus;
import org.springframework.data.jpa.domain.Specification;

public final class FarmerSpecifications {

    private FarmerSpecifications() {
    }

    public static Specification<Farmer> hasName(String name) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("fullName")),
                        "%" + name.toLowerCase() + "%");
    }

    public static Specification<Farmer> hasMembershipNumber(String membershipNumber) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("membershipNumber")),
                        "%" + membershipNumber.toLowerCase() + "%");
    }

    public static Specification<Farmer> hasCooperativeId(Long cooperativeId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("cooperative").get("id"),
                        cooperativeId);
    }

    public static Specification<Farmer> hasStatus(FarmerStatus status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Farmer> hasDistrictId(Long districtId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("cooperative").get("district").get("id"),
                        districtId);
    }
}