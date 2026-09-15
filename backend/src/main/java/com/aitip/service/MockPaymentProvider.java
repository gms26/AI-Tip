package com.aitip.service;

import com.aitip.entity.PaymentSession;

public class MockPaymentProvider implements PaymentProvider {

    @Override
    public String createSession(PaymentSession session) {
        // Deterministic failure simulation
        if ("FAIL_CREATE".equals(session.getDescription())) {
            throw new RuntimeException("Mock payment provider failed to create session.");
        }
        return "mock_sess_" + session.getId().toString();
    }

    @Override
    public boolean checkStatus(String providerSessionId) {
        // Mock always returns true (completed) unless simulated otherwise if needed
        return true; 
    }

    @Override
    public void cancelSession(String providerSessionId) {
        // Always succeeds for mock
        if (providerSessionId == null || providerSessionId.isEmpty()) {
            throw new RuntimeException("Invalid provider session ID for cancellation");
        }
    }
}
