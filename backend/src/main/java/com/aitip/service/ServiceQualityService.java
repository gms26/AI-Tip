package com.aitip.service;

import com.aitip.dto.ServiceQuality;
import com.aitip.dto.ServiceQualityStatsResponse;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.exception.ResourceNotFoundException;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Calculates deterministic service quality statistics from the authenticated user's tips.
 *
 * <p><b>Design principles:</b></p>
 * <ul>
 *   <li>All statistics are deterministic and backend-calculated — no AI involved</li>
 *   <li>Only explicit user ratings are considered (NULL = not rated, excluded)</li>
 *   <li>Historical tips (serviceQuality = NULL) are silently excluded</li>
 *   <li>User isolation: statistics are always scoped to one user</li>
 * </ul>
 *
 * <p><b>Tie-breaking rule for mostCommon:</b>
 * Fixed enum order POOR &lt; AVERAGE &lt; GOOD &lt; EXCELLENT.
 * On a count tie, the higher quality wins.
 * Implemented with {@code >=} comparison so the last enum in iteration order
 * wins when counts are equal.</p>
 *
 * <p><b>Rounding:</b>
 * All average tip percentages use {@code BigDecimal.divide} with scale=2
 * and {@code RoundingMode.HALF_UP}. No double/float arithmetic.</p>
 */
@Service
public class ServiceQualityService {

    private static final Logger log = LoggerFactory.getLogger(ServiceQualityService.class);

    private final TipRepository tipRepository;
    private final UserRepository userRepository;

    public ServiceQualityService(TipRepository tipRepository, UserRepository userRepository) {
        this.tipRepository = tipRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns service quality statistics for the authenticated user.
     *
     * <p><b>Empty state:</b> If the user has no rated tips, returns a response
     * with {@code totalRatedTips = 0} and an empty map. This is not an error.</p>
     *
     * @param email The authenticated user's email (from JWT)
     * @return Deterministic service quality statistics
     */
    @Transactional(readOnly = true)
    public ServiceQualityStatsResponse getStats(String email) {
        UUID userId = resolveUserId(email);

        List<Tip> ratedTips = tipRepository.findByUserIdAndServiceQualityIsNotNull(userId);

        log.debug("Calculating service quality stats for user: {}, rated tips: {}", email, ratedTips.size());

        if (ratedTips.isEmpty()) {
            return emptyResponse();
        }

        return buildResponse(ratedTips);
    }

    // =============================================
    // Private helpers
    // =============================================

    /**
     * Returns an empty-state response when the user has no rated tips.
     * Distinct from an error — "No service ratings yet" is a valid state.
     */
    private ServiceQualityStatsResponse emptyResponse() {
        return new ServiceQualityStatsResponse(
                0, 0, 0, 0, 0,
                null,
                Map.of()
        );
    }

    /**
     * Builds the full statistics response from the list of rated tips.
     */
    private ServiceQualityStatsResponse buildResponse(List<Tip> ratedTips) {
        // Group tips by quality using an EnumMap (preserves enum declaration order)
        Map<ServiceQuality, List<Tip>> byQuality = new EnumMap<>(ServiceQuality.class);
        for (ServiceQuality q : ServiceQuality.values()) {
            byQuality.put(q, new java.util.ArrayList<>());
        }
        for (Tip tip : ratedTips) {
            byQuality.get(tip.getServiceQuality()).add(tip);
        }

        int poorCount      = byQuality.get(ServiceQuality.POOR).size();
        int averageCount   = byQuality.get(ServiceQuality.AVERAGE).size();
        int goodCount      = byQuality.get(ServiceQuality.GOOD).size();
        int excellentCount = byQuality.get(ServiceQuality.EXCELLENT).size();

        // Determine mostCommon using tie-breaking rule:
        // iterate enum in order (POOR < AVERAGE < GOOD < EXCELLENT)
        // use >= so the later (higher quality) enum wins on ties
        ServiceQuality mostCommon = null;
        int maxCount = 0;
        for (ServiceQuality q : ServiceQuality.values()) {
            int count = byQuality.get(q).size();
            if (count >= maxCount && count > 0) {
                maxCount = count;
                mostCommon = q;
            }
        }

        // Build averageTipPercentageByQuality map — only include qualities with data
        Map<ServiceQuality, BigDecimal> avgByQuality = new EnumMap<>(ServiceQuality.class);
        for (ServiceQuality q : ServiceQuality.values()) {
            List<Tip> tips = byQuality.get(q);
            if (!tips.isEmpty()) {
                BigDecimal sum = tips.stream()
                        .map(Tip::getTipPercentage)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                // Rule 2: BigDecimal divide, scale=2, HALF_UP — never use double
                BigDecimal avg = sum.divide(BigDecimal.valueOf(tips.size()), 2, RoundingMode.HALF_UP);
                avgByQuality.put(q, avg);
            }
        }

        return new ServiceQualityStatsResponse(
                ratedTips.size(),
                poorCount,
                averageCount,
                goodCount,
                excellentCount,
                mostCommon,
                avgByQuality
        );
    }

    private UUID resolveUserId(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId();
    }
}
