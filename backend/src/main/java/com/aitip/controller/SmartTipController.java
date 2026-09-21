package com.aitip.controller;

import com.aitip.dto.*;
import com.aitip.service.AiProvider;
import com.aitip.service.SmartTipFeedbackService;
import com.aitip.service.SmartTipPromptBuilder;
import com.aitip.service.SmartTipService;
import com.aitip.service.SmartTipDecisionMemoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Context-Aware Smart Tip Assistant & Feedback Controller (Day 31 & Day 32).
 *
 * <p>Exposes:
 * <ul>
 *   <li>{@code POST /api/smart-tip}: Advisory tip suggestions before tip commitment.</li>
 *   <li>{@code POST /api/smart-tip/feedback}: Record explicit tipping decision feedback.</li>
 *   <li>{@code GET /api/smart-tip/feedback}: Retrieve behavioral feedback summary.</li>
 * </ul>
 * </p>
 *
 * <p>Strict Boundaries:
 * <ul>
 *   <li>Never auto-saves tips or modifies calculations silently.</li>
 *   <li>User identity strictly derived from {@link Authentication#getName()}.</li>
 *   <li>Currency isolation strictly enforced.</li>
 *   <li>Groq AI is strictly optional, explanatory only, with a 3-second non-blocking deadline.</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/smart-tip")
public class SmartTipController {

    private static final Logger log = LoggerFactory.getLogger(SmartTipController.class);

    private final SmartTipService smartTipService;
    private final SmartTipFeedbackService smartTipFeedbackService;
    private final SmartTipDecisionMemoryService smartTipDecisionMemoryService;
    private final SmartTipPromptBuilder promptBuilder;
    private final AiProvider AiProvider;

    public SmartTipController(SmartTipService smartTipService,
                               SmartTipFeedbackService smartTipFeedbackService,
                               SmartTipDecisionMemoryService smartTipDecisionMemoryService,
                               SmartTipPromptBuilder promptBuilder,
                               @Autowired(required = false) AiProvider AiProvider) {
        this.smartTipService = smartTipService;
        this.smartTipFeedbackService = smartTipFeedbackService;
        this.smartTipDecisionMemoryService = smartTipDecisionMemoryService;
        this.promptBuilder = promptBuilder;
        this.AiProvider = AiProvider;
    }

    @PostMapping
    public ResponseEntity<SmartTipResponse> getSmartTip(
            Authentication authentication,
            @Valid @RequestBody SmartTipRequest request) {

        String email = authentication.getName();
        log.debug("Generating smart tip suggestions for user={}, bill={}, currency={}",
                email, request.billAmount(), request.currency());

        SmartTipResponse response = smartTipService.getSmartTip(email, request);

        if (AiProvider != null) {
            try {
                String prompt = promptBuilder.buildPrompt(response);
                CompletableFuture<String> future = CompletableFuture.supplyAsync(
                        () -> AiProvider.getRecommendation(prompt));
                String aiExplanation = future.get(3, TimeUnit.SECONDS);
                if (aiExplanation != null && !aiExplanation.isBlank()) {
                    response = response.withAiExplanation(aiExplanation.trim());
                }
            } catch (Exception e) {
                log.debug("Groq explanation generation skipped or timed out: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/feedback")
    public ResponseEntity<SmartTipFeedbackResponse> recordFeedback(
            Authentication authentication,
            @Valid @RequestBody SmartTipFeedbackRequest request) {

        String email = authentication.getName();
        log.debug("Recording smart tip feedback for user={}, currency={}", email, request.currency());

        SmartTipFeedbackResponse response = smartTipFeedbackService.recordFeedback(email, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/feedback")
    public ResponseEntity<SmartTipFeedbackSummaryResponse> getFeedbackSummary(
            Authentication authentication,
            @RequestParam("currency") String currency) {

        String email = authentication.getName();
        log.debug("Retrieving smart tip feedback summary for user={}, currency={}", email, currency);

        SmartTipFeedbackSummaryResponse response = smartTipFeedbackService.getSummary(email, currency);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/decision-memory")
    public ResponseEntity<SmartTipDecisionMemoryResponse> getDecisionMemory(
            Authentication authentication,
            @RequestParam("currency") String currency) {

        String email = authentication.getName();
        log.debug("Retrieving smart tip decision memory for user={}, currency={}", email, currency);

        SmartTipDecisionMemoryResponse response = smartTipDecisionMemoryService.getDecisionMemory(email, currency);
        return ResponseEntity.ok(response);
    }
}
