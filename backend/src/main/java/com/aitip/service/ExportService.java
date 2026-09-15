package com.aitip.service;

import com.aitip.dto.TipExportRecord;
import com.aitip.entity.Tip;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final ObjectMapper jsonMapper;

    /**
     * Converts a list of Tip entities to TipExportRecord DTOs.
     */
    public List<TipExportRecord> mapToExportRecords(List<Tip> tips) {
        return tips.stream().map(tip -> new TipExportRecord(
                tip.getId(),
                tip.getCreatedAt(),
                tip.getRestaurantName(),
                tip.getBillAmount(),
                tip.getTipPercentage(),
                tip.getTipAmount(),
                tip.getTotalAmount(),
                tip.getCurrency(),
                tip.getServiceQuality()
        )).collect(Collectors.toList());
    }

    /**
     * Converts TipExportRecord list to a CSV string using Jackson CsvMapper.
     */
    public String exportTipsToCsv(List<TipExportRecord> records) {
        try {
            CsvMapper csvMapper = new CsvMapper();
            csvMapper.registerModule(new JavaTimeModule());
            csvMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            CsvSchema schema = csvMapper.schemaFor(TipExportRecord.class).withHeader();
            return csvMapper.writer(schema).writeValueAsString(records);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to generate CSV data", e);
        }
    }

    /**
     * Converts an Object to a JSON string.
     */
    public String exportToJson(Object data) {
        try {
            return jsonMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to generate JSON data", e);
        }
    }
}
