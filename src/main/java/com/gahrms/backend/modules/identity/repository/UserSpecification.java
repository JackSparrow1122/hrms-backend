package com.gahrms.backend.modules.identity.repository;

import com.gahrms.backend.modules.identity.entity.Role;
import com.gahrms.backend.modules.identity.entity.ScopeType;
import com.gahrms.backend.modules.identity.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class UserSpecification {

    private UserSpecification() {}

    public static Specification<User> filter(
            Long companyId,
            String search,
            String roleName,
            ScopeType scopeType,
            Boolean active
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (companyId != null) {
                predicates.add(cb.equal(root.get("companyId"), companyId));
            }

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate emailPredicate = cb.like(cb.lower(root.get("email")), pattern);
                Predicate firstNamePredicate = cb.like(cb.lower(root.get("firstName")), pattern);
                Predicate lastNamePredicate = cb.like(cb.lower(root.get("lastName")), pattern);
                predicates.add(cb.or(emailPredicate, firstNamePredicate, lastNamePredicate));
            }

            if (StringUtils.hasText(roleName)) {
                Join<User, Role> roleJoin = root.join("roles");
                predicates.add(cb.equal(cb.upper(roleJoin.get("name")), roleName.trim().toUpperCase()));
            }

            if (scopeType != null) {
                predicates.add(cb.equal(root.get("scopeType"), scopeType));
            }

            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
