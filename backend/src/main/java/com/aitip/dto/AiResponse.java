package com.aitip.dto;

import java.util.List;

/**
 * Maps to the response payload from OpenAI-compatible APIs (like Groq).
 */
public record AiResponse(List<Choice> choices) {

    public record Choice(Message message) {}

    public record Message(String role, String content) {}
    
    /**
     * Extracts the text from the first choice's message.
     * Safe navigation returns null if the response is unexpectedly structured.
     */
    public String extractText() {
        if (choices != null && !choices.isEmpty()) {
            Choice choice = choices.get(0);
            if (choice != null && choice.message() != null) {
                return choice.message().content();
            }
        }
        return null;
    }
}
