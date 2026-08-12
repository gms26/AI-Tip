package com.aitip.service;

import com.aitip.dto.CreateTipRequest;
import com.aitip.dto.ServiceQuality;
import com.aitip.dto.TipResponse;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.exception.ResourceNotFoundException;
import com.aitip.mapper.TipMapper;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service orchestrating CRUD operations for tips.
 *
 * <p><b>Purpose:</b> Manages the persistence lifecycle of tips.
 * Relies on TipCalculationService for math, and TipRepository for DB access.</p>
 */
@Service
public class TipService {

    private static final Logger log = LoggerFactory.getLogger(TipService.class);

    private final TipRepository tipRepository;
    private final UserRepository userRepository;
    private final TipCalculationService calculationService;

    public TipService(TipRepository tipRepository, UserRepository userRepository, TipCalculationService calculationService) {
        this.tipRepository = tipRepository;
        this.userRepository = userRepository;
        this.calculationService = calculationService;
    }

    /**
     * Calculates and saves a new tip for the authenticated user.
     *
     * @param request The tip details (includes serviceQuality from Day 6)
     * @param email   The authenticated user's email
     * @return Saved tip response
     */
    @Transactional
    public TipResponse calculateAndSave(CreateTipRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BigDecimal tipAmount = calculationService.calculateTipAmount(request.billAmount(), request.tipPercentage());
        BigDecimal totalAmount = calculationService.calculateTotalAmount(request.billAmount(), tipAmount);

        Tip tip = TipMapper.toEntity(request, user, tipAmount, totalAmount);
        Tip savedTip = tipRepository.save(tip);

        log.info("Saved tip for user: {}, bill amount: {}, serviceQuality: {}", email, request.billAmount(), request.serviceQuality());
        return TipMapper.toTipResponse(savedTip);
    }

    /**
     * Retrieves paginated tips for the authenticated user.
     *
     * @param email    The authenticated user's email
     * @param pageable Pagination and sorting
     * @return Page of tip responses
     */
    @Transactional(readOnly = true)
    public Page<TipResponse> getUserTips(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return tipRepository.findByUserId(user.getId(), pageable)
                .map(TipMapper::toTipResponse);
    }

    /**
     * Retrieves a single tip, ensuring it belongs to the authenticated user.
     *
     * @param tipId The tip ID
     * @param email The authenticated user's email
     * @return Tip response
     */
    @Transactional(readOnly = true)
    public TipResponse getTipById(UUID tipId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Tip tip = tipRepository.findByIdAndUserId(tipId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tip not found or access denied"));

        return TipMapper.toTipResponse(tip);
    }

    /**
     * Deletes a tip, ensuring it belongs to the authenticated user.
     *
     * @param tipId The tip ID
     * @param email The authenticated user's email
     */
    @Transactional
    public void deleteTip(UUID tipId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!tipRepository.existsByIdAndUserId(tipId, user.getId())) {
             throw new ResourceNotFoundException("Tip not found or access denied");
        }

        tipRepository.deleteById(tipId);
        log.info("Deleted tip {} for user: {}", tipId, email);
    }

    /**
     * Updates the service quality of an existing tip for the authenticated user.
     *
     * <p><b>Ownership:</b> {@code findByIdAndUserId} enforces that the tip belongs
     * to the authenticated user. If the tip does not exist or belongs to another
     * user, a 404 is returned. This prevents leaking whether another user's tip
     * exists (security: no enumeration).</p>
     *
     * @param tipId          The tip UUID to update
     * @param email          The authenticated user's email
     * @param serviceQuality The new service quality rating
     * @return Updated tip response
     */
    @Transactional
    public TipResponse updateServiceQuality(UUID tipId, String email, ServiceQuality serviceQuality) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Tip tip = tipRepository.findByIdAndUserId(tipId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tip not found or access denied"));

        tip.setServiceQuality(serviceQuality);
        Tip savedTip = tipRepository.save(tip);

        log.info("Updated service quality to {} for tip {} (user: {})", serviceQuality, tipId, email);
        return TipMapper.toTipResponse(savedTip);
    }
}
