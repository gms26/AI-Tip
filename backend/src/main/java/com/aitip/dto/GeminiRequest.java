package com.aitip.dto;

import java.util.List;

/**
 * Maps to the request payload for Gemini 1.5 API.
 */
public record GeminiRequest(List<Content> contents) {
    
    public record Content(List<Part> parts) {}
    
    public record Part(String text) {}
    
    /**
     * Factory method for creating a simple text prompt request.
     */
    public static GeminiRequest forTextPrompt(String prompt) {
        return new GeminiRequest(List.of(new Content(List.of(new Part(prompt)))));
    }
}
