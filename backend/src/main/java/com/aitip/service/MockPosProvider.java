package com.aitip.service;

import com.aitip.dto.PosBillRequest;
import com.aitip.dto.PosBillResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MockPosProvider implements PosProvider {

    @Override
    public PosBillResponse getBill(PosBillRequest request) {
        // Deterministic failure simulation
        if ("FAIL_POS".equals(request.getExternalReference())) {
            throw new RuntimeException("Mock POS provider failed to retrieve bill.");
        }

        PosBillResponse response = new PosBillResponse();
        response.setBillId("mock_bill_" + UUID.randomUUID().toString().substring(0, 8));
        response.setRestaurantName(request.getRestaurantName());
        response.setBillAmount(request.getBillAmount());
        response.setCurrency(request.getCurrency());
        response.setSource("Mock POS Sandbox");
        response.setTimestamp(LocalDateTime.now());

        return response;
    }
}
