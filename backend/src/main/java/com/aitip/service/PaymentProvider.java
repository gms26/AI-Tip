package com.aitip.service;

import com.aitip.entity.PaymentSession;
import java.util.UUID;

public interface PaymentProvider {
    
    /**
     * Creates a payment session with the provider.
     * @param session The payment session entity.
     * @return provider's session/reference ID if necessary, or the same session ID.
     */
    String createSession(PaymentSession session);
    
    /**
     * Retrieves the status of a payment session from the provider.
     * @param providerSessionId The provider's session ID.
     * @return true if completed, false otherwise (simplified for now).
     */
    boolean checkStatus(String providerSessionId);
    
    /**
     * Cancels the payment session with the provider.
     * @param providerSessionId The provider's session ID.
     */
    void cancelSession(String providerSessionId);
}
