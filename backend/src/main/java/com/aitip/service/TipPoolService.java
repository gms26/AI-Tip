package com.aitip.service;

import com.aitip.dto.CreateTipPoolRequest;
import com.aitip.dto.PoolMemberResponse;
import com.aitip.dto.TipPoolResponse;
import com.aitip.entity.PoolMember;
import com.aitip.entity.TipPool;
import com.aitip.entity.TipPoolStatus;
import com.aitip.entity.User;
import com.aitip.exception.ResourceNotFoundException;
import com.aitip.repository.TipPoolRepository;
import com.aitip.repository.UserRepository;
import com.aitip.util.CurrencyValidationUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TipPoolService {

    private final TipPoolRepository tipPoolRepository;
    private final UserRepository userRepository;
    private final TipPoolCalculationService calculationService;

    public TipPoolService(TipPoolRepository tipPoolRepository, UserRepository userRepository, TipPoolCalculationService calculationService) {
        this.tipPoolRepository = tipPoolRepository;
        this.userRepository = userRepository;
        this.calculationService = calculationService;
    }

    @Transactional
    public TipPoolResponse createPool(CreateTipPoolRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String normalizedCurrency = CurrencyValidationUtil.normalizeAndValidate(request.currency());

        // Delegate business math entirely to the stateless service
        List<PoolMemberResponse> calculatedMembers = calculationService.calculateDistribution(
                request.totalTip(),
                request.distributionType(),
                request.members()
        );

        TipPool pool = TipPool.builder()
                .user(user)
                .restaurantName(request.restaurantName())
                .totalTip(request.totalTip())
                .currency(normalizedCurrency)
                .distributionType(request.distributionType())
                .status(TipPoolStatus.DRAFT)
                .build();

        for (PoolMemberResponse memberResp : calculatedMembers) {
            PoolMember member = PoolMember.builder()
                    .memberName(memberResp.name())
                    .allocationPercentage(memberResp.allocationPercentage())
                    .allocatedAmount(memberResp.allocatedAmount())
                    .build();
            pool.addMember(member);
        }

        TipPool savedPool = tipPoolRepository.save(pool);
        return mapToResponse(savedPool);
    }

    @Transactional(readOnly = true)
    public List<TipPoolResponse> getUserPools(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return tipPoolRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TipPoolResponse getPoolById(UUID poolId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TipPool pool = tipPoolRepository.findByIdAndUserId(poolId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("TipPool not found"));

        return mapToResponse(pool);
    }

    @Transactional
    public TipPoolResponse finalizePool(UUID poolId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TipPool pool = tipPoolRepository.findByIdAndUserId(poolId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("TipPool not found"));

        if (pool.getStatus() == TipPoolStatus.FINALIZED) {
            throw new IllegalStateException("Pool is already finalized.");
        }

        pool.setStatus(TipPoolStatus.FINALIZED);
        TipPool savedPool = tipPoolRepository.save(pool);
        return mapToResponse(savedPool);
    }

    @Transactional
    public void deleteDraftPool(UUID poolId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TipPool pool = tipPoolRepository.findByIdAndUserId(poolId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("TipPool not found"));

        if (pool.getStatus() == TipPoolStatus.FINALIZED) {
            throw new IllegalStateException("Cannot delete a finalized tip pool.");
        }

        tipPoolRepository.delete(pool);
    }

    private TipPoolResponse mapToResponse(TipPool pool) {
        List<PoolMemberResponse> memberResponses = pool.getMembers().stream()
                .map(m -> new PoolMemberResponse(m.getMemberName(), m.getAllocationPercentage(), m.getAllocatedAmount()))
                .toList();

        return new TipPoolResponse(
                pool.getId(),
                pool.getRestaurantName(),
                pool.getTotalTip(),
                pool.getCurrency(),
                pool.getDistributionType(),
                pool.getStatus(),
                memberResponses,
                pool.getCreatedAt(),
                pool.getUpdatedAt()
        );
    }
}
