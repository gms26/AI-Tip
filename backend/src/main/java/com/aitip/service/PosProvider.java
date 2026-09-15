package com.aitip.service;

import com.aitip.dto.PosBillRequest;
import com.aitip.dto.PosBillResponse;

public interface PosProvider {
    
    /**
     * Retrieves a bill from the POS system based on a request.
     * @param request The request to retrieve a bill.
     * @return The retrieved bill response.
     */
    PosBillResponse getBill(PosBillRequest request);
}
