package com.aitip.service;

import com.aitip.dto.PosBillRequest;
import com.aitip.dto.PosBillResponse;
import org.springframework.stereotype.Service;

@Service
public class PosService {

    private final PosProvider posProvider;

    public PosService(PosProvider posProvider) {
        this.posProvider = posProvider;
    }

    public PosBillResponse getBill(PosBillRequest request) {
        return posProvider.getBill(request);
    }
}
