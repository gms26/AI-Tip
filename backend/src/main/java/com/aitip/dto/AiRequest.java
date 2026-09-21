package com.aitip.dto;

import java.util.List;

/**
 * Maps to the request payload for OpenAI-compatible APIs (like Groq).
 */
public record AiRequest(String model, List<Message> messages) {
    
    public record Message(String role, Object content) {}
    
    /**
     * Factory method for creating a simple text prompt request.
     */
    public static AiRequest forTextPrompt(String model, String prompt) {
        return new AiRequest(model, List.of(new Message("user", prompt)));
    }

    /**
     * Factory method for creating an image analysis request (Vision payload).
     */
    public static AiRequest forImagePrompt(String model, String prompt, String mimeType, String base64Image) {
        String dataUri = "data:" + mimeType + ";base64," + base64Image;
        
        var textContent = java.util.Map.of("type", "text", "text", prompt);
        var imageContent = java.util.Map.of("type", "image_url", "image_url", java.util.Map.of("url", dataUri));
        
        return new AiRequest(model, List.of(new Message("user", List.of(textContent, imageContent))));
    }
}
