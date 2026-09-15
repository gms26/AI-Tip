package com.aitip.service;

import com.aitip.dto.PosBillRequest;
import com.aitip.dto.PosBillResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PosServiceTest {

    @Mock
    private PosProvider posProvider;

    @InjectMocks
    private PosService posService;

    private PosBillRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new PosBillRequest();
        validRequest.setRestaurantName("Test POS Rest");
        validRequest.setBillAmount(new BigDecimal("150.00"));
        validRequest.setCurrency("USD");
    }

    @Test
    void getBill_ValidRequest_Success() {
        PosBillResponse mockResponse = new PosBillResponse();
        mockResponse.setBillId("pos_123");
        mockResponse.setRestaurantName(validRequest.getRestaurantName());
        mockResponse.setBillAmount(validRequest.getBillAmount());
        mockResponse.setCurrency(validRequest.getCurrency());
        
        when(posProvider.getBill(any(PosBillRequest.class))).thenReturn(mockResponse);

        PosBillResponse response = posService.getBill(validRequest);

        assertNotNull(response);
        assertEquals("pos_123", response.getBillId());
        assertEquals(validRequest.getRestaurantName(), response.getRestaurantName());
        assertEquals(validRequest.getBillAmount(), response.getBillAmount());
    }
}
