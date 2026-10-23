package com.gahrms.backend.modules.employee.repository;

import com.gahrms.backend.modules.employee.entity.Employee;
import com.gahrms.backend.modules.employee.entity.EmploymentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class EmployeeSpecification {

    public static Specification<Employee> filter(
            Long companyId,
            String search,
            Long departmentId,
            Long branchId,
            Long designationId,
            EmploymentStatus status,
            List<Long> allowedEmployeeIds,
            List<Long> excludedEmployeeIds
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (companyId != null) {
                predicates.add(cb.equal(root.get("company").get("id"), companyId));
            }

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate codeMatch = cb.like(cb.lower(root.get("employeeCode")), searchPattern);
                Predicate firstNameMatch = cb.like(cb.lower(root.get("firstName")), searchPattern);
                Predicate lastNameMatch = cb.like(cb.lower(root.get("lastName")), searchPattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), searchPattern);

                predicates.add(cb.or(codeMatch, firstNameMatch, lastNameMatch, emailMatch));
            }

            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }

            if (branchId != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), branchId));
            }

            if (designationId != null) {
                predicates.add(cb.equal(root.get("designation").get("id"), designationId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (allowedEmployeeIds != null) {
                if (allowedEmployeeIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("id").in(allowedEmployeeIds));
                }
            }

            if (excludedEmployeeIds != null && !excludedEmployeeIds.isEmpty()) {
                predicates.add(cb.not(root.get("id").in(excludedEmployeeIds)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
