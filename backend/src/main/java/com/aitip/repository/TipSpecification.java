package com.aitip.repository;

import com.aitip.dto.ExportFilterRequest;
import com.aitip.entity.Tip;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TipSpecification {

    public static Specification<Tip> withFilters(UUID userId, ExportFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. userId is always required to guarantee data isolation
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (filter == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            // 2. startDate filter
            if (filter.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.startDate()));
            }

            // 3. endDate filter
            if (filter.endDate() != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), filter.endDate()));
            }

            // 4. restaurantName filter
            if (filter.restaurantName() != null && !filter.restaurantName().trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("restaurantName")), filter.restaurantName().toLowerCase()));
            }

            // 5. currency filter
            if (filter.currency() != null && !filter.currency().trim().isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("currency")), filter.currency().toUpperCase()));
            }

            // 6. serviceQuality filter
            if (filter.serviceQuality() != null) {
                predicates.add(cb.equal(root.get("serviceQuality"), filter.serviceQuality()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
