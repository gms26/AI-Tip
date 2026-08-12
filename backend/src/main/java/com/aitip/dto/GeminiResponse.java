package com.aitip.dto;

import java.util.List;

/**
 * Maps to the response payload from the Gemini 1.5 API.
 */
public record GeminiResponse(List<Candidate> candidates) {

    public record Candidate(Content content) {}

    public record Content(List<Part> parts) {}

    public record Part(String text) {}
    
    /**
     * Extracts the text from the first candidate.
     * Safe navigation returns null if the response is unexpectedly structured.
     */
    public String extractText() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate candidate = candidates.get(0);
            if (candidate != null && candidate.content() != null && candidate.content().parts() != null && !candidate.content().parts().isEmpty()) {
                Part part = candidate.content().parts().get(0);
                if (part != null) {
                    return part.text();
                }
            }
        }
        return null;
    }
}
