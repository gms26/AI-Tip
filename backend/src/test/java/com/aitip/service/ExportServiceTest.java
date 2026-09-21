package com.aitip.service;

import com.aitip.dto.ServiceQuality;
import com.aitip.dto.TipExportRecord;
import com.aitip.entity.Tip;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExportServiceTest {

    private ExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new ExportService(new ObjectMapper());
    }

    @Test
    void mapToExportRecords_MapsFieldsCorrectly() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Tip tip = Tip.builder()
                .id(id)
                .createdAt(now)
                .restaurantName("CafÃ© 123")
                .billAmount(new BigDecimal("100.00"))
                .tipPercentage(new BigDecimal("20.00"))
                .tipAmount(new BigDecimal("20.00"))
                .totalAmount(new BigDecimal("120.00"))
                .currency("USD")
                .serviceQuality(ServiceQuality.EXCELLENT)
                .build();

        List<TipExportRecord> records = exportService.mapToExportRecords(List.of(tip));

        assertThat(records).hasSize(1);
        TipExportRecord record = records.get(0);
        assertThat(record.id()).isEqualTo(id);
        assertThat(record.createdAt()).isEqualTo(now);
        assertThat(record.restaurantName()).isEqualTo("CafÃ© 123");
        assertThat(record.billAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(record.tipPercentage()).isEqualTo(new BigDecimal("20.00"));
        assertThat(record.tipAmount()).isEqualTo(new BigDecimal("20.00"));
        assertThat(record.totalAmount()).isEqualTo(new BigDecimal("120.00"));
        assertThat(record.currency()).isEqualTo("USD");
        assertThat(record.serviceQuality()).isEqualTo(ServiceQuality.EXCELLENT);
    }

    @Test
    void exportTipsToCsv_FormatsCorrectlyAndPreservesDecimals() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2023, 10, 1, 12, 0);
        TipExportRecord record = new TipExportRecord(
                id,
                now,
                "The \"Best\" Place, Inc",
                new BigDecimal("17.30"),
                new BigDecimal("15.00"),
                new BigDecimal("2.59"),
                new BigDecimal("19.89"),
                "USD",
                ServiceQuality.AVERAGE
        );

        String csv = exportService.exportTipsToCsv(List.of(record));

        assertThat(csv).contains("id,createdAt,restaurantName,billAmount,tipPercentage,tipAmount,totalAmount,currency,serviceQuality");
        assertThat(csv).contains(id.toString());
        assertThat(csv).contains("2023-10-01T12:00:00");
        assertThat(csv).contains("\"The \"\"Best\"\" Place, Inc\"");
        assertThat(csv).contains("17.30");
        assertThat(csv).contains("2.59");
        assertThat(csv).contains("19.89");
        assertThat(csv).contains("USD");
        assertThat(csv).contains("AVERAGE");
    }
}
