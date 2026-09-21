package com.aitip.service;

import com.aitip.dto.ReceiptAnalysisResponse;
import com.aitip.exception.AiServiceException;
import com.aitip.exception.ReceiptOcrException;
import com.aitip.exception.ReceiptOcrUnavailableException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.Currency;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReceiptOcrService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final AiProvider AiProvider;
    private final ObjectMapper objectMapper;
    private final Set<String> validCurrencyCodes;

    private static final String PROMPT = 
            "Analyze this receipt image and extract the subtotal or total bill amount, the restaurant name, and the currency. " +
            "Do not perform any tip or tax calculations. " +
            "Respond ONLY with a valid JSON object matching this exact schema: " +
            "{ \"billAmount\": number, \"restaurantName\": \"string\", \"currency\": \"string\" }. " +
            "If currency is unknown, set it to null. If bill amount or restaurant name is missing, set them to null.";

    public ReceiptOcrService(AiProvider AiProvider, ObjectMapper objectMapper) {
        this.AiProvider = AiProvider;
        this.objectMapper = objectMapper;
        this.validCurrencyCodes = Currency.getAvailableCurrencies().stream()
                .map(Currency::getCurrencyCode)
                .collect(Collectors.toSet());
    }

    public ReceiptAnalysisResponse analyzeReceipt(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ReceiptOcrException("File is missing or empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ReceiptOcrException("File exceeds the 5 MB limit.");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new ReceiptOcrException("Unsupported file type. Only JPEG, PNG, and WebP are allowed.");
        }

        String base64Image;
        try {
            base64Image = Base64.getEncoder().encodeToString(file.getBytes());
        } catch (IOException e) {
            throw new ReceiptOcrException("Failed to read the uploaded file.");
        }

        String AiResponseRaw;
        try {
            AiResponseRaw = AiProvider.analyzeImage(PROMPT, mimeType, base64Image);
        } catch (AiServiceException e) {
            // Map AI exception (timeout, rate limit, parse error inside groq client) to 503
            throw new ReceiptOcrUnavailableException("Receipt analysis is temporarily unavailable.");
        }

        return parseAndValidate(AiResponseRaw);
    }

    private ReceiptAnalysisResponse parseAndValidate(String rawResponse) {
        // Clean up potential markdown formatting from Groq
        if (rawResponse != null) {
            rawResponse = rawResponse.replaceAll("```json", "").replaceAll("```", "").trim();
        }

        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(rawResponse);
        } catch (JsonProcessingException e) {
            throw new ReceiptOcrUnavailableException("Receipt analysis is temporarily unavailable.");
        }

        if (jsonNode == null || !jsonNode.isObject()) {
            throw new ReceiptOcrUnavailableException("Receipt analysis is temporarily unavailable.");
        }

        BigDecimal billAmount = null;
        if (jsonNode.hasNonNull("billAmount")) {
            billAmount = new BigDecimal(jsonNode.get("billAmount").asText());
        }

        String restaurantName = null;
        if (jsonNode.hasNonNull("restaurantName")) {
            restaurantName = jsonNode.get("restaurantName").asText().trim();
        }

        String currency = null;
        if (jsonNode.hasNonNull("currency")) {
            currency = jsonNode.get("currency").asText().trim().toUpperCase();
        }

        if (billAmount == null || billAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReceiptOcrException("Could not extract a valid positive bill amount from the receipt.");
        }

        if (restaurantName == null || restaurantName.isBlank()) {
            throw new ReceiptOcrException("Could not extract a restaurant name from the receipt.");
        }

        if (currency == null || !validCurrencyCodes.contains(currency)) {
            throw new ReceiptOcrException("Could not detect a valid currency from the receipt.");
        }

        return new ReceiptAnalysisResponse(billAmount, restaurantName, currency);
    }
}
