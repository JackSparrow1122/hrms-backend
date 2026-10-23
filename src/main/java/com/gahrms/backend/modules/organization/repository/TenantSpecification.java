package com.gahrms.backend.modules.organization.repository;

import com.gahrms.backend.modules.organization.entity.Company;
import com.gahrms.backend.modules.organization.entity.TenantStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class TenantSpecification {

    private TenantSpecification() {}

    public static Specification<Company> filter(
            String search,
            TenantStatus status,
            String subscriptionPlan
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate namePred = cb.like(cb.lower(root.get("name")), pattern);
                Predicate codePred = cb.like(cb.lower(root.get("code")), pattern);
                Predicate domainPred = cb.like(cb.lower(root.get("domain")), pattern);
                Predicate emailPred = cb.like(cb.lower(root.get("contactEmail")), pattern);
                predicates.add(cb.or(namePred, codePred, domainPred, emailPred));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (StringUtils.hasText(subscriptionPlan)) {
                predicates.add(cb.equal(cb.upper(root.get("subscriptionPlan")), subscriptionPlan.trim().toUpperCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
