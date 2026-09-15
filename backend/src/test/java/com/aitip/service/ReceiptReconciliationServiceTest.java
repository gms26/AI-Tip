package com.aitip.service;

import com.aitip.dto.ReceiptReconciliationRequest;
import com.aitip.dto.ReceiptReconciliationResponse;
import com.aitip.dto.ReceiptReconciliationStatus;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptReconciliationServiceTest {

    @Mock
    private TipRepository tipRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReceiptReconciliationService service;

    private User testUser;
    private final String EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail(EMAIL);
    }

    @Test
    void reconcile_EmptyHistory_ReturnsNoMatch() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(Collections.emptyList());

        ReceiptReconciliationRequest request = new ReceiptReconciliationRequest(
                new BigDecimal("45.67"), "Italian Place", "USD", null, null);

        ReceiptReconciliationResponse response = service.reconcile(EMAIL, request);

        assertThat(response.status()).isEqualTo(ReceiptReconciliationStatus.NO_MATCH);
        assertThat(response.matchCount()).isZero();
        assertThat(response.matches()).isEmpty();
    }

    @Test
    void reconcile_ExactMatchWithoutDate_ReturnsStrongMatch100() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);
        
        Tip tip = new Tip();
        tip.setId(UUID.randomUUID());
        tip.setRestaurantName("Italian Place");
        tip.setBillAmount(new BigDecimal("45.67"));
        tip.setCurrency("USD");
        tip.setCreatedAt(LocalDateTime.now().minusDays(1));

        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(List.of(tip));

        ReceiptReconciliationRequest request = new ReceiptReconciliationRequest(
                new BigDecimal("45.67"), "Italian Place", "USD", null, null);

        ReceiptReconciliationResponse response = service.reconcile(EMAIL, request);

        assertThat(response.status()).isEqualTo(ReceiptReconciliationStatus.STRONG_MATCH);
        assertThat(response.matchCount()).isEqualTo(1);
        assertThat(response.matches().get(0).matchScore()).isEqualTo(100);
        assertThat(response.matches().get(0).matchReasons()).contains(
                "Currency matches",
                "Bill amount matches within $0.01",
                "Restaurant name matches"
        );
    }

    @Test
    void reconcile_ExactMatchWithDate_ReturnsStrongMatch100() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);
        
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Tip tip = new Tip();
        tip.setId(UUID.randomUUID());
        tip.setRestaurantName("Italian Place");
        tip.setBillAmount(new BigDecimal("45.67"));
        tip.setCurrency("USD");
        tip.setCreatedAt(yesterday.atStartOfDay());

        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(List.of(tip));

        ReceiptReconciliationRequest request = new ReceiptReconciliationRequest(
                new BigDecimal("45.67"), "Italian Place", "USD", yesterday, null);

        ReceiptReconciliationResponse response = service.reconcile(EMAIL, request);

        assertThat(response.status()).isEqualTo(ReceiptReconciliationStatus.STRONG_MATCH);
        assertThat(response.matches().get(0).matchScore()).isEqualTo(100); // 110 raw -> 100
        assertThat(response.matches().get(0).matchReasons()).contains("Receipt date matches");
    }

    @Test
    void reconcile_CurrencyMismatch_Excluded() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);
        
        Tip tip = new Tip(); // INR tip
        tip.setRestaurantName("Italian Place");
        tip.setBillAmount(new BigDecimal("45.67"));
        tip.setCurrency("INR");
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(List.of(tip));

        ReceiptReconciliationRequest request = new ReceiptReconciliationRequest( // USD request
                new BigDecimal("45.67"), "Italian Place", "USD", null, null);

        ReceiptReconciliationResponse response = service.reconcile(EMAIL, request);
        assertThat(response.status()).isEqualTo(ReceiptReconciliationStatus.NO_MATCH);
    }

    @Test
    void reconcile_RestaurantCaseInsensitivity_WhitespaceNormalization() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);
        
        Tip tip = new Tip();
        tip.setId(UUID.randomUUID());
        tip.setRestaurantName("ITALIAN   PLACE");
        tip.setBillAmount(new BigDecimal("45.67"));
        tip.setCurrency("USD");
        tip.setCreatedAt(LocalDateTime.now());

        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(List.of(tip));

        ReceiptReconciliationRequest request = new ReceiptReconciliationRequest(
                new BigDecimal("45.67"), " italian place ", "USD", null, null);

        ReceiptReconciliationResponse response = service.reconcile(EMAIL, request);

        assertThat(response.status()).isEqualTo(ReceiptReconciliationStatus.STRONG_MATCH);
        assertThat(response.matches().get(0).matchScore()).isEqualTo(100);
    }

    @Test
    void reconcile_AmountDifferenceExactlyOneCent_Matches() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);
        
        Tip tip = new Tip();
        tip.setId(UUID.randomUUID());
        tip.setRestaurantName("Italian Place");
        tip.setBillAmount(new BigDecimal("45.66")); // historical is 45.66
        tip.setCurrency("USD");
        tip.setCreatedAt(LocalDateTime.now());

        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(List.of(tip));

        ReceiptReconciliationRequest request = new ReceiptReconciliationRequest(
                new BigDecimal("45.67"), "Italian Place", "USD", null, null); // request is 45.67

        ReceiptReconciliationResponse response = service.reconcile(EMAIL, request);

        assertThat(response.status()).isEqualTo(ReceiptReconciliationStatus.STRONG_MATCH);
        assertThat(response.matches().get(0).matchScore()).isEqualTo(100);
    }

    @Test
    void reconcile_AmountDifferenceMoreThanOneCent_NoAmountMatch() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);
        
        Tip tip = new Tip();
        tip.setId(UUID.randomUUID());
        tip.setRestaurantName("Italian Place");
        tip.setBillAmount(new BigDecimal("45.65")); // diff is 0.02
        tip.setCurrency("USD");
        tip.setCreatedAt(LocalDateTime.now());

        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(List.of(tip));

        ReceiptReconciliationRequest request = new ReceiptReconciliationRequest(
                new BigDecimal("45.67"), "Italian Place", "USD", null, null);

        ReceiptReconciliationResponse response = service.reconcile(EMAIL, request);

        // Currency (40) + Restaurant (20) = 60
        assertThat(response.status()).isEqualTo(ReceiptReconciliationStatus.POSSIBLE_MATCH);
        assertThat(response.matches().get(0).matchScore()).isEqualTo(60);
    }

    @Test
    void reconcile_PossibleMatch_NoRestaurant() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);
        
        Tip tip = new Tip();
        tip.setId(UUID.randomUUID());
        tip.setRestaurantName("Burger Joint");
        tip.setBillAmount(new BigDecimal("45.67"));
        tip.setCurrency("USD");
        tip.setCreatedAt(LocalDateTime.now());

        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(List.of(tip));

        ReceiptReconciliationRequest request = new ReceiptReconciliationRequest(
                new BigDecimal("45.67"), "Italian Place", "USD", null, null);

        ReceiptReconciliationResponse response = service.reconcile(EMAIL, request);

        // Currency (40) + Amount (40) = 80 -> STRONG_MATCH (wait, 80 is strong)
        assertThat(response.status()).isEqualTo(ReceiptReconciliationStatus.STRONG_MATCH);
        assertThat(response.matches().get(0).matchScore()).isEqualTo(80);
    }

    @Test
    void reconcile_NoMatchClassification() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);
        
        Tip tip = new Tip();
        tip.setId(UUID.randomUUID());
        tip.setRestaurantName("Burger Joint");
        tip.setBillAmount(new BigDecimal("12.00"));
        tip.setCurrency("USD");
        tip.setCreatedAt(LocalDateTime.now());

        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(List.of(tip));

        ReceiptReconciliationRequest request = new ReceiptReconciliationRequest(
                new BigDecimal("45.67"), "Italian Place", "USD", null, null);

        ReceiptReconciliationResponse response = service.reconcile(EMAIL, request);

        // Currency only = 40 (<50 means excluded from matches entirely)
        assertThat(response.status()).isEqualTo(ReceiptReconciliationStatus.NO_MATCH);
        assertThat(response.matchCount()).isZero();
    }

    @Test
    void reconcile_MultipleCandidates_SortsCorrectly() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);
        
        Tip t1 = new Tip();
        t1.setId(UUID.randomUUID());
        t1.setRestaurantName("Italian Place");
        t1.setBillAmount(new BigDecimal("45.67"));
        t1.setCurrency("USD");
        t1.setCreatedAt(LocalDateTime.now().minusDays(2)); // Score 100, older

        Tip t2 = new Tip();
        t2.setId(UUID.randomUUID());
        t2.setRestaurantName("Burger Joint");
        t2.setBillAmount(new BigDecimal("45.67"));
        t2.setCurrency("USD");
        t2.setCreatedAt(LocalDateTime.now().minusDays(1)); // Score 80

        Tip t3 = new Tip();
        t3.setId(UUID.randomUUID());
        t3.setRestaurantName("Italian Place");
        t3.setBillAmount(new BigDecimal("45.67"));
        t3.setCurrency("USD");
        t3.setCreatedAt(LocalDateTime.now().minusDays(1)); // Score 100, newer

        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(List.of(t1, t2, t3));

        ReceiptReconciliationRequest request = new ReceiptReconciliationRequest(
                new BigDecimal("45.67"), "Italian Place", "USD", null, null);

        ReceiptReconciliationResponse response = service.reconcile(EMAIL, request);

        assertThat(response.matchCount()).isEqualTo(3);
        assertThat(response.matches().get(0).tipId()).isEqualTo(t3.getId()); // 100, newest
        assertThat(response.matches().get(1).tipId()).isEqualTo(t1.getId()); // 100, older
        assertThat(response.matches().get(2).tipId()).isEqualTo(t2.getId()); // 80
    }
}
