package com.aitip.service;

import com.aitip.dto.TipHistoryRecord;
import com.aitip.dto.TipHistorySearchRequest;
import com.aitip.dto.TipHistorySearchResponse;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.exception.ResourceNotFoundException;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import com.aitip.specification.TipSpecification;
import com.aitip.util.CurrencyValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TipHistorySearchService {

    private final TipRepository tipRepository;
    private final UserRepository userRepository;

    // Allowed sort fields
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "tipPercentage", "tipAmount", "billAmount", "restaurantName"
    );

    @Transactional(readOnly = true)
    public TipHistorySearchResponse searchHistory(String email, TipHistorySearchRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.currency() != null && !request.currency().isBlank()) {
            CurrencyValidationUtil.normalizeAndValidate(request.currency());
        }

        Sort sort = buildSort(request.sortBy(), request.sortDirection());
        
        int page = request.page() != null ? request.page() : 0;
        int size = request.size() != null ? request.size() : 20;
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Tip> tipPage = tipRepository.findAll(
                TipSpecification.buildSearchSpecification(request, user.getId()),
                pageable
        );

        List<TipHistoryRecord> content = tipPage.getContent().stream()
                .map(this::mapToRecord)
                .collect(Collectors.toList());

        return new TipHistorySearchResponse(
                content,
                tipPage.getNumber(),
                tipPage.getSize(),
                tipPage.getTotalElements(),
                tipPage.getTotalPages(),
                tipPage.hasNext(),
                tipPage.hasPrevious()
        );
    }

    private Sort buildSort(String sortByRaw, String sortDirectionRaw) {
        String sortBy = "createdAt";
        if (sortByRaw != null && !sortByRaw.isBlank() && ALLOWED_SORT_FIELDS.contains(sortByRaw)) {
            sortBy = sortByRaw;
        }

        Sort.Direction direction = Sort.Direction.DESC;
        if (sortDirectionRaw != null && sortDirectionRaw.equalsIgnoreCase("ASC")) {
            direction = Sort.Direction.ASC;
        }

        return Sort.by(direction, sortBy);
    }

    private TipHistoryRecord mapToRecord(Tip tip) {
        return new TipHistoryRecord(
                tip.getId(),
                tip.getRestaurantName(),
                tip.getBillAmount(),
                tip.getTipAmount(),
                tip.getTipPercentage(),
                tip.getCurrency(),
                tip.getServiceQuality(),
                tip.getCreatedAt()
        );
    }
}
