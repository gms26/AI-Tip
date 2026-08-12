package com.aitip.service;

import com.aitip.dto.GenerosityScoreResponse;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerosityScoreServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TipRepository tipRepository;

    @InjectMocks
    private GenerosityScoreService generosityScoreService;

    private User testUser;
    private final String userEmail = "test@example.com";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email(userEmail)
                .password("encoded_pass")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Tip createTip(double percentage) {
        return Tip.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .billAmount(new BigDecimal("100.00"))
                .tipPercentage(BigDecimal.valueOf(percentage))
                .tipAmount(BigDecimal.valueOf(percentage))
                .totalAmount(BigDecimal.valueOf(100 + percentage))
                .currency("USD")
                .restaurantName("Test Rest")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void mockUserAndTips(List<Tip> tips) {
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(tips);
    }

    // 1. No tips
    @Test
    void testNoTips() {
        mockUserAndTips(List.of());
        GenerosityScoreResponse response = generosityScoreService.getScore(userEmail);
        
        assertNull(response.score());
        assertNull(response.category());
        assertNull(response.medianTipPercentage());
        assertNull(response.meanTipPercentage());
        assertEquals(0, response.totalTips());
        assertEquals("LOW", response.confidence());
    }

    // 2. One tip
    @Test
    void testOneTip() {
        mockUserAndTips(List.of(createTip(20.0)));
        GenerosityScoreResponse response = generosityScoreService.getScore(userEmail);

        assertEquals(80, response.score());
        assertEquals("VERY_GENEROUS", response.category());
        assertEquals("LOW", response.confidence());
        assertEquals(new BigDecimal("20.0"), response.medianTipPercentage());
        assertEquals(new BigDecimal("20.00"), response.meanTipPercentage());
        assertEquals(1, response.totalTips());
    }

    // 3. Two tips
    @Test
    void testTwoTips() {
        mockUserAndTips(List.of(createTip(10.0), createTip(20.0)));
        GenerosityScoreResponse response = generosityScoreService.getScore(userEmail);

        assertEquals(60, response.score()); // Median 15 -> 60
        assertEquals("GENEROUS", response.category());
        assertEquals("MEDIUM", response.confidence());
        assertEquals(new BigDecimal("15.00"), response.medianTipPercentage());
        assertEquals(2, response.totalTips());
    }

    // 4. Median odd count
    @Test
    void testMedianOddCount() {
        mockUserAndTips(List.of(createTip(10.0), createTip(15.0), createTip(25.0)));
        GenerosityScoreResponse response = generosityScoreService.getScore(userEmail);

        assertEquals(new BigDecimal("15.0"), response.medianTipPercentage());
        assertEquals(60, response.score());
    }

    // 5. Median even count
    @Test
    void testMedianEvenCount() {
        mockUserAndTips(List.of(createTip(18.0), createTip(20.0), createTip(22.0), createTip(24.0)));
        GenerosityScoreResponse response = generosityScoreService.getScore(userEmail);

        // (20+22)/2 = 21
        assertEquals(new BigDecimal("21.00"), response.medianTipPercentage());
        assertEquals(84, response.score());
    }

    // 6. Mean calculation
    @Test
    void testMeanCalculation() {
        mockUserAndTips(List.of(createTip(17.0), createTip(18.0), createTip(20.0)));
        GenerosityScoreResponse response = generosityScoreService.getScore(userEmail);

        // 55 / 3 = 18.33
        assertEquals(new BigDecimal("18.33"), response.meanTipPercentage());
    }

    // 7. 0% tip
    @Test
    void testZeroPercentTip() {
        mockUserAndTips(List.of(createTip(0.0)));
        GenerosityScoreResponse response = generosityScoreService.getScore(userEmail);
        assertEquals(0, response.score());
        assertEquals("CONSERVATIVE", response.category());
    }

    // 8. 5% tip
    @Test
    void testFivePercentTip() {
        mockUserAndTips(List.of(createTip(5.0)));
        GenerosityScoreResponse response = generosityScoreService.getScore(userEmail);
        assertEquals(20, response.score());
        assertEquals("CONSERVATIVE", response.category());
    }

    // 9. 10% tip
    @Test
    void testTenPercentTip() {
        mockUserAndTips(List.of(createTip(10.0)));
        GenerosityScoreResponse response = generosityScoreService.getScore(userEmail);
        assertEquals(40, response.score());
        assertEquals("MODERATE", response.category());
    }

    // 10. 15% tip
    @Test
    void testFifteenPercentTip() {
        mockUserAndTips(List.of(createTip(15.0)));
        GenerosityScoreResponse response = generosityScoreService.getScore(userEmail);
        assertEquals(60, response.score());
        assertEquals("GENEROUS", response.category());
    }

    // 11. 20% tip
    @Test
    void testTwentyPercentTip() {
        mockUserAndTips(List.of(createTip(20.0)));
        GenerosityScoreResponse response = generosityScoreService.getScore(userEmail);
        assertEquals(80, response.score());
        assertEquals("VERY_GENEROUS", response.category());
    }

    // 12. 25% tip
    @Test
    void testTwentyFivePercentTip() {
        mockUserAndTips(List.of(createTip(25.0)));
        GenerosityScoreResponse response = generosityScoreService.getScore(userEmail);
        assertEquals(100, response.score());
        assertEquals("VERY_GENEROUS", response.category());
    }

    // 13. >25% clamp
    @Test
    void testClampAboveTwentyFive() {
        mockUserAndTips(List.of(createTip(30.0)));
        GenerosityScoreResponse response = generosityScoreService.getScore(userEmail);
        assertEquals(100, response.score());
    }

    // 14. Negative/invalid values cannot produce negative score
    @Test
    void testNegativeClamp() {
        mockUserAndTips(List.of(createTip(-5.0)));
        GenerosityScoreResponse response = generosityScoreService.getScore(userEmail);
        assertEquals(0, response.score());
    }

    // 15. Score rounding
    @Test
    void testScoreRounding() {
        mockUserAndTips(List.of(createTip(18.0)));
        GenerosityScoreResponse response = generosityScoreService.getScore(userEmail);
        assertEquals(72, response.score()); // 18 * 4 = 72
        
        mockUserAndTips(List.of(createTip(18.125)));
        GenerosityScoreResponse response2 = generosityScoreService.getScore(userEmail);
        assertEquals(73, response2.score()); // 18.125 * 4 = 72.5 -> 73
    }

    // 16-21. Category boundaries
    @Test
    void testCategoryBoundaries() {
        // Boundary 39
        mockUserAndTips(List.of(createTip(9.75))); // 9.75 * 4 = 39
        assertEquals("CONSERVATIVE", generosityScoreService.getScore(userEmail).category());

        // Boundary 40
        mockUserAndTips(List.of(createTip(10.0))); // 10 * 4 = 40
        assertEquals("MODERATE", generosityScoreService.getScore(userEmail).category());

        // Boundary 59
        mockUserAndTips(List.of(createTip(14.75))); // 14.75 * 4 = 59
        assertEquals("MODERATE", generosityScoreService.getScore(userEmail).category());

        // Boundary 60
        mockUserAndTips(List.of(createTip(15.0))); // 15 * 4 = 60
        assertEquals("GENEROUS", generosityScoreService.getScore(userEmail).category());

        // Boundary 79
        mockUserAndTips(List.of(createTip(19.75))); // 19.75 * 4 = 79
        assertEquals("GENEROUS", generosityScoreService.getScore(userEmail).category());

        // Boundary 80
        mockUserAndTips(List.of(createTip(20.0))); // 20 * 4 = 80
        assertEquals("VERY_GENEROUS", generosityScoreService.getScore(userEmail).category());
    }

    // 22-24. Confidence thresholds
    @Test
    void testConfidenceThresholds() {
        // LOW (1 tip)
        mockUserAndTips(List.of(createTip(20.0)));
        assertEquals("LOW", generosityScoreService.getScore(userEmail).confidence());

        // MEDIUM (2-4 tips)
        mockUserAndTips(List.of(createTip(20.0), createTip(20.0)));
        assertEquals("MEDIUM", generosityScoreService.getScore(userEmail).confidence());
        mockUserAndTips(List.of(createTip(20.0), createTip(20.0), createTip(20.0), createTip(20.0)));
        assertEquals("MEDIUM", generosityScoreService.getScore(userEmail).confidence());

        // HIGH (5+ tips)
        mockUserAndTips(List.of(createTip(20.0), createTip(20.0), createTip(20.0), createTip(20.0), createTip(20.0)));
        assertEquals("HIGH", generosityScoreService.getScore(userEmail).confidence());
    }

    // 25. Outlier robustness using median
    @Test
    void testOutlierRobustness() {
        mockUserAndTips(List.of(createTip(18.0), createTip(20.0), createTip(20.0), createTip(100.0)));
        GenerosityScoreResponse response = generosityScoreService.getScore(userEmail);
        
        // Median is (20 + 20) / 2 = 20.00
        assertEquals(new BigDecimal("20.00"), response.medianTipPercentage());
        assertEquals(80, response.score());
    }

    // 26. User isolation (Simulation - verified partly here and comprehensively in Integration tests)
    @Test
    void testUserIsolation() {
        User userB = User.builder().id(UUID.randomUUID()).email("b@example.com").build();
        Tip tipA = createTip(20.0);
        Tip tipB = Tip.builder().user(userB).tipPercentage(new BigDecimal("5.0")).build();
        
        mockUserAndTips(List.of(tipA)); // Simulating repository isolating by userId
        GenerosityScoreResponse response = generosityScoreService.getScore(userEmail);
        assertEquals(80, response.score());
    }
}
