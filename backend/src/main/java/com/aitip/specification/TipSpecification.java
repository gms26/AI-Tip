package com.aitip.specification;

import com.aitip.dto.TipHistorySearchRequest;
import com.aitip.entity.Tip;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TipSpecification {

    public static Specification<Tip> buildSearchSpecification(TipHistorySearchRequest request, UUID userId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory Identity Isolation
            predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));

            // 2. Optional Filters
            if (request.restaurantName() != null && !request.restaurantName().isBlank()) {
                String normalizedSearch = request.restaurantName().trim().toLowerCase();
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("restaurantName")),
                        "%" + normalizedSearch + "%"
                ));
            }

            if (request.currency() != null && !request.currency().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("currency"), request.currency().trim()));
            }

            if (request.serviceQuality() != null) {
                predicates.add(criteriaBuilder.equal(root.get("serviceQuality"), request.serviceQuality()));
            }

            if (request.minTipPercentage() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("tipPercentage"), request.minTipPercentage()));
            }

            if (request.maxTipPercentage() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("tipPercentage"), request.maxTipPercentage()));
            }

            if (request.minBillAmount() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("billAmount"), request.minBillAmount()));
            }

            if (request.maxBillAmount() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("billAmount"), request.maxBillAmount()));
            }

            if (request.startDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), request.startDate().atStartOfDay()));
            }

            if (request.endDate() != null) {
                // Ensure records strictly before the NEXT day (meaning inclusive of the end date)
                predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), request.endDate().plusDays(1).atStartOfDay()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
