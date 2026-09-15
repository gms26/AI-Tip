package com.aitip.service;

import com.aitip.dto.PoolMemberRequest;
import com.aitip.dto.PoolMemberResponse;
import com.aitip.entity.DistributionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TipPoolCalculationServiceTest {

    private TipPoolCalculationService service;

    @BeforeEach
    void setUp() {
        service = new TipPoolCalculationService();
    }

    @Test
    void calculateEqualDistribution_2People() {
        BigDecimal totalTip = new BigDecimal("10.00");
        List<PoolMemberRequest> members = List.of(
                new PoolMemberRequest("A", null),
                new PoolMemberRequest("B", null)
        );

        List<PoolMemberResponse> result = service.calculateDistribution(totalTip, DistributionType.EQUAL, members);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("5.00"), result.get(0).allocatedAmount());
        assertEquals(new BigDecimal("50.00000"), result.get(0).allocationPercentage());
        assertEquals(new BigDecimal("5.00"), result.get(1).allocatedAmount());
    }

    @Test
    void calculateEqualDistribution_3People() {
        BigDecimal totalTip = new BigDecimal("10.00");
        List<PoolMemberRequest> members = List.of(
                new PoolMemberRequest("A", null),
                new PoolMemberRequest("B", null),
                new PoolMemberRequest("C", null)
        );

        List<PoolMemberResponse> result = service.calculateDistribution(totalTip, DistributionType.EQUAL, members);

        // 10 / 3 = 3.33 each, remainder 0.01 goes to A
        assertEquals(new BigDecimal("3.34"), result.get(0).allocatedAmount());
        assertEquals(new BigDecimal("3.33"), result.get(1).allocatedAmount());
        assertEquals(new BigDecimal("3.33"), result.get(2).allocatedAmount());

        // 100 / 3 = 33.33333, remainder goes to A
        assertEquals(new BigDecimal("33.33334"), result.get(0).allocationPercentage());
        assertEquals(new BigDecimal("33.33333"), result.get(1).allocationPercentage());
        assertEquals(new BigDecimal("33.33333"), result.get(2).allocationPercentage());
        
        // Sum invariant
        BigDecimal sum = result.stream().map(PoolMemberResponse::allocatedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(totalTip, sum);
    }

    @Test
    void calculateEqualDistribution_7People() {
        BigDecimal totalTip = new BigDecimal("100.00");
        List<PoolMemberRequest> members = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            members.add(new PoolMemberRequest("M" + i, null));
        }

        List<PoolMemberResponse> result = service.calculateDistribution(totalTip, DistributionType.EQUAL, members);

        BigDecimal sum = result.stream().map(PoolMemberResponse::allocatedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, sum.compareTo(totalTip));
    }

    @Test
    void calculateEqualDistribution_OneCent() {
        BigDecimal totalTip = new BigDecimal("0.01");
        List<PoolMemberRequest> members = List.of(
                new PoolMemberRequest("A", null),
                new PoolMemberRequest("B", null)
        );

        List<PoolMemberResponse> result = service.calculateDistribution(totalTip, DistributionType.EQUAL, members);

        assertEquals(new BigDecimal("0.01"), result.get(0).allocatedAmount());
        assertEquals(new BigDecimal("0.00"), result.get(1).allocatedAmount());
        
        BigDecimal sum = result.stream().map(PoolMemberResponse::allocatedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, sum.compareTo(totalTip));
    }

    @Test
    void calculatePercentageDistribution_Exactly100() {
        BigDecimal totalTip = new BigDecimal("100.00");
        List<PoolMemberRequest> members = List.of(
                new PoolMemberRequest("A", new BigDecimal("50.00")),
                new PoolMemberRequest("B", new BigDecimal("30.00")),
                new PoolMemberRequest("C", new BigDecimal("20.00"))
        );

        List<PoolMemberResponse> result = service.calculateDistribution(totalTip, DistributionType.PERCENTAGE, members);

        assertEquals(new BigDecimal("50.00"), result.get(0).allocatedAmount());
        assertEquals(new BigDecimal("30.00"), result.get(1).allocatedAmount());
        assertEquals(new BigDecimal("20.00"), result.get(2).allocatedAmount());
    }

    @Test
    void calculatePercentageDistribution_99_99_Rejects() {
        BigDecimal totalTip = new BigDecimal("100.00");
        List<PoolMemberRequest> members = List.of(
                new PoolMemberRequest("A", new BigDecimal("50.00")),
                new PoolMemberRequest("B", new BigDecimal("49.99"))
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
                () -> service.calculateDistribution(totalTip, DistributionType.PERCENTAGE, members));
        assertTrue(ex.getMessage().contains("must equal exactly 100"));
    }

    @Test
    void calculatePercentageDistribution_100_01_Rejects() {
        BigDecimal totalTip = new BigDecimal("100.00");
        List<PoolMemberRequest> members = List.of(
                new PoolMemberRequest("A", new BigDecimal("50.00")),
                new PoolMemberRequest("B", new BigDecimal("50.01"))
        );

        assertThrows(IllegalArgumentException.class, 
                () -> service.calculateDistribution(totalTip, DistributionType.PERCENTAGE, members));
    }

    @Test
    void calculatePercentageDistribution_Negative_Rejects() {
        BigDecimal totalTip = new BigDecimal("100.00");
        List<PoolMemberRequest> members = List.of(
                new PoolMemberRequest("A", new BigDecimal("110.00")),
                new PoolMemberRequest("B", new BigDecimal("-10.00"))
        );

        assertThrows(IllegalArgumentException.class, 
                () -> service.calculateDistribution(totalTip, DistributionType.PERCENTAGE, members));
    }

    @Test
    void calculatePercentageDistribution_Over100_Rejects() {
        BigDecimal totalTip = new BigDecimal("100.00");
        List<PoolMemberRequest> members = List.of(
                new PoolMemberRequest("A", new BigDecimal("105.00")),
                new PoolMemberRequest("B", new BigDecimal("0.00"))
        );

        assertThrows(IllegalArgumentException.class, 
                () -> service.calculateDistribution(totalTip, DistributionType.PERCENTAGE, members));
    }

    @Test
    void validateInputs_LessThan2_Rejects() {
        BigDecimal totalTip = new BigDecimal("10.00");
        List<PoolMemberRequest> members = List.of(new PoolMemberRequest("A", null));
        assertThrows(IllegalArgumentException.class, 
                () -> service.calculateDistribution(totalTip, DistributionType.EQUAL, members));
    }

    @Test
    void validateInputs_MoreThan20_Rejects() {
        BigDecimal totalTip = new BigDecimal("10.00");
        List<PoolMemberRequest> members = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            members.add(new PoolMemberRequest("M" + i, null));
        }
        assertThrows(IllegalArgumentException.class, 
                () -> service.calculateDistribution(totalTip, DistributionType.EQUAL, members));
    }

    @Test
    void validateInputs_DuplicateNames_Rejects() {
        BigDecimal totalTip = new BigDecimal("10.00");
        List<PoolMemberRequest> members = List.of(
                new PoolMemberRequest("John", null),
                new PoolMemberRequest("john", null)
        );
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
                () -> service.calculateDistribution(totalTip, DistributionType.EQUAL, members));
        assertTrue(ex.getMessage().contains("Duplicate"));
    }

    @Test
    void validateInputs_ZeroTip_Rejects() {
        BigDecimal totalTip = new BigDecimal("0.00");
        List<PoolMemberRequest> members = List.of(
                new PoolMemberRequest("A", null),
                new PoolMemberRequest("B", null)
        );
        assertThrows(IllegalArgumentException.class, 
                () -> service.calculateDistribution(totalTip, DistributionType.EQUAL, members));
    }
}
