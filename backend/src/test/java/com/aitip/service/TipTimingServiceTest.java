package com.aitip.service;

import com.aitip.dto.ServiceQuality;
import com.aitip.dto.TipTimingRequest;
import com.aitip.dto.TipTimingResponse;
import com.aitip.dto.TipTimingState;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipTimingServiceTest {

    @Mock
    private TipRepository tipRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TipTimingService tipTimingService;

    private User testUser;
    private TipTimingRequest validRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");

        validRequest = TipTimingRequest.builder()
                .billAmount(new BigDecimal("100.00"))
                .tipPercentage(new BigDecimal("20.00"))
                .restaurantName("Italian Place")
                .serviceQuality(ServiceQuality.EXCELLENT)
                .saved(false)
                .build();
    }

    @Test
    void getTipTiming_whenSaved_returnsNotReady() {
        when(userService.getUserByEmail(anyString())).thenReturn(testUser);

        validRequest.setSaved(true);
        TipTimingResponse response = tipTimingService.getTipTiming("test@example.com", validRequest);

        assertEquals(TipTimingState.NOT_READY, response.getState());
        assertEquals("LOW", response.getConfidence());
        assertEquals("Tip is already saved.", response.getReason());
    }

    @Test
    void getTipTiming_whenNoBillAmount_returnsNotReady() {
        when(userService.getUserByEmail(anyString())).thenReturn(testUser);

        validRequest.setBillAmount(null);
        TipTimingResponse response = tipTimingService.getTipTiming("test@example.com", validRequest);

        assertEquals(TipTimingState.NOT_READY, response.getState());
    }

    @Test
    void getTipTiming_whenNoRestaurant_returnsNotReady() {
        when(userService.getUserByEmail(anyString())).thenReturn(testUser);

        validRequest.setRestaurantName("");
        TipTimingResponse response = tipTimingService.getTipTiming("test@example.com", validRequest);

        assertEquals(TipTimingState.NOT_READY, response.getState());
    }

    @Test
    void getTipTiming_whenNoHistory_returnsGoodTime() {
        when(userService.getUserByEmail(anyString())).thenReturn(testUser);
        when(tipRepository.findByUserIdAndRestaurantNameIgnoreCase(testUser.getId(), "Italian Place"))
                .thenReturn(new ArrayList<>());

        TipTimingResponse response = tipTimingService.getTipTiming("test@example.com", validRequest);

        assertEquals(TipTimingState.GOOD_TIME, response.getState());
        assertEquals("LOW", response.getConfidence());
        assertEquals(0, response.getVisitCount());
        assertFalse(response.getHasRestaurantHistory());
    }

    @Test
    void getTipTiming_whenOneVisit_returnsRecommendedLowConfidence() {
        when(userService.getUserByEmail(anyString())).thenReturn(testUser);
        
        Tip pastTip = new Tip();
        pastTip.setTipPercentage(new BigDecimal("15.00"));
        pastTip.setCreatedAt(LocalDateTime.now().minusDays(1));
        
        when(tipRepository.findByUserIdAndRestaurantNameIgnoreCase(testUser.getId(), "Italian Place"))
                .thenReturn(List.of(pastTip));

        TipTimingResponse response = tipTimingService.getTipTiming("test@example.com", validRequest);

        assertEquals(TipTimingState.RECOMMENDED, response.getState());
        assertEquals("LOW", response.getConfidence());
        assertEquals(1, response.getVisitCount());
        assertTrue(response.getHasRestaurantHistory());
        assertEquals(new BigDecimal("15.00"), response.getLastTipPercentage());
        assertEquals(new BigDecimal("15.00"), response.getMedianTipPercentage());
    }

    @Test
    void getTipTiming_whenFiveVisits_returnsRecommendedHighConfidence() {
        when(userService.getUserByEmail(anyString())).thenReturn(testUser);
        
        List<Tip> pastTips = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Tip t = new Tip();
            t.setTipPercentage(new BigDecimal("20.00"));
            t.setCreatedAt(LocalDateTime.now().minusDays(i));
            pastTips.add(t);
        }
        
        when(tipRepository.findByUserIdAndRestaurantNameIgnoreCase(testUser.getId(), "Italian Place"))
                .thenReturn(pastTips);

        TipTimingResponse response = tipTimingService.getTipTiming("test@example.com", validRequest);

        assertEquals(TipTimingState.RECOMMENDED, response.getState());
        assertEquals("HIGH", response.getConfidence());
        assertEquals(5, response.getVisitCount());
        assertTrue(response.getHasRestaurantHistory());
    }
}
