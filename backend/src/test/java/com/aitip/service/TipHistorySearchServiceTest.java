package com.aitip.service;

import com.aitip.dto.ServiceQuality;
import com.aitip.dto.TipHistorySearchRequest;
import com.aitip.dto.TipHistorySearchResponse;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.exception.ResourceNotFoundException;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TipHistorySearchServiceTest {

    @Mock
    private TipRepository tipRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TipHistorySearchService tipHistorySearchService;

    private User testUser;
    private Tip testTip;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .password("hash")
                .build();

        testTip = Tip.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .restaurantName("Italian Place")
                .billAmount(new BigDecimal("100.00"))
                .tipAmount(new BigDecimal("20.00"))
                .tipPercentage(new BigDecimal("20.00"))
                .totalAmount(new BigDecimal("120.00"))
                .currency("USD")
                .serviceQuality(ServiceQuality.GOOD)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void searchHistory_ValidRequest_ReturnsResponse() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(tipRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testTip)));

        TipHistorySearchRequest req = new TipHistorySearchRequest(
                null, null, null, null, null, null, null, null, null, null, null, 0, 10
        );

        TipHistorySearchResponse res = tipHistorySearchService.searchHistory("test@example.com", req);

        assertThat(res.content()).hasSize(1);
        assertThat(res.content().get(0).restaurantName()).isEqualTo("Italian Place");
        assertThat(res.totalElements()).isEqualTo(1);
    }

    @Test
    void searchHistory_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        TipHistorySearchRequest req = new TipHistorySearchRequest(
                null, null, null, null, null, null, null, null, null, null, null, 0, 10
        );

        assertThrows(ResourceNotFoundException.class, () ->
                tipHistorySearchService.searchHistory("notfound@example.com", req)
        );
    }

    @Test
    void searchHistory_InvalidCurrency_ThrowsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        TipHistorySearchRequest req = new TipHistorySearchRequest(
                null, "INVALID", null, null, null, null, null, null, null, null, null, 0, 10
        );

        assertThrows(IllegalArgumentException.class, () ->
                tipHistorySearchService.searchHistory("test@example.com", req)
        );
    }

    @Test
    void searchHistory_PaginationAndSorting_UsesCorrectPageable() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(tipRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        TipHistorySearchRequest req = new TipHistorySearchRequest(
                null, null, null, null, null, null, null, null, null, "tipPercentage", "ASC", 2, 50
        );

        tipHistorySearchService.searchHistory("test@example.com", req);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(tipRepository).findAll(any(Specification.class), captor.capture());

        Pageable usedPageable = captor.getValue();
        assertThat(usedPageable.getPageNumber()).isEqualTo(2);
        assertThat(usedPageable.getPageSize()).isEqualTo(50);
        assertThat(usedPageable.getSort().getOrderFor("tipPercentage")).isNotNull();
        assertThat(usedPageable.getSort().getOrderFor("tipPercentage").isAscending()).isTrue();
    }
    
    // Add more cases to hit the 25+ requirement. 
    // Testing the inner specification fully usually requires an in-memory DB or DataJpaTest, 
    // but we can add various filter configurations here to ensure service parses them correctly.
}
