package com.aitip.repository;

import com.aitip.dto.ExportFilterRequest;
import com.aitip.dto.ServiceQuality;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TipSpecificationTest {

    @Autowired
    private TipRepository tipRepository;

    @Autowired
    private UserRepository userRepository;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = User.builder().email("a@test.com").password("hash").name("A").build();
        userB = User.builder().email("b@test.com").password("hash").name("B").build();
        userRepository.saveAll(List.of(userA, userB));

        Tip tip1 = Tip.builder().user(userA).restaurantName("Rest A").billAmount(new BigDecimal("10")).tipPercentage(new BigDecimal("10")).tipAmount(new BigDecimal("1")).totalAmount(new BigDecimal("11")).currency("USD").serviceQuality(ServiceQuality.EXCELLENT).build();
        // Override createdAt to test dates
        tip1.setCreatedAt(LocalDateTime.of(2023, 1, 15, 12, 0));
        
        Tip tip2 = Tip.builder().user(userA).restaurantName("Rest B").billAmount(new BigDecimal("20")).tipPercentage(new BigDecimal("20")).tipAmount(new BigDecimal("4")).totalAmount(new BigDecimal("24")).currency("EUR").serviceQuality(ServiceQuality.AVERAGE).build();
        tip2.setCreatedAt(LocalDateTime.of(2023, 2, 15, 12, 0));

        Tip tip3 = Tip.builder().user(userB).restaurantName("Rest A").billAmount(new BigDecimal("30")).tipPercentage(new BigDecimal("10")).tipAmount(new BigDecimal("3")).totalAmount(new BigDecimal("33")).currency("USD").serviceQuality(ServiceQuality.POOR).build();

        tipRepository.saveAll(List.of(tip1, tip2, tip3));
    }

    @Test
    void withFilters_IsolatesByUser() {
        ExportFilterRequest filter = new ExportFilterRequest(null, null, null, null, null);
        Specification<Tip> spec = TipSpecification.withFilters(userA.getId(), filter);

        List<Tip> results = tipRepository.findAll(spec);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(Tip::getRestaurantName).containsExactlyInAnyOrder("Rest A", "Rest B");
    }

    @Test
    void withFilters_FiltersByRestaurantName() {
        ExportFilterRequest filter = new ExportFilterRequest(null, null, "rest a", null, null);
        Specification<Tip> spec = TipSpecification.withFilters(userA.getId(), filter);

        List<Tip> results = tipRepository.findAll(spec);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRestaurantName()).isEqualTo("Rest A");
    }

    @Test
    void withFilters_FiltersByCurrency() {
        ExportFilterRequest filter = new ExportFilterRequest(null, null, null, "eur", null);
        Specification<Tip> spec = TipSpecification.withFilters(userA.getId(), filter);

        List<Tip> results = tipRepository.findAll(spec);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCurrency()).isEqualTo("EUR");
    }

    @Test
    void withFilters_FiltersByDate() {
        LocalDateTime start = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2023, 12, 31, 23, 59);
        ExportFilterRequest filter = new ExportFilterRequest(start, end, null, null, null);
        Specification<Tip> spec = TipSpecification.withFilters(userA.getId(), filter);

        List<Tip> results = tipRepository.findAll(spec);

        assertThat(results).hasSize(2);
    }
}
