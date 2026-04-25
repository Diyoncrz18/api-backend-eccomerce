package com.backend.demo.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents one message in the chat history (either from user or AI).
 * Used for multi-turn conversation context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    /** "user" or "ai" */
    private String role;

    /** Plain text content of the message */
    private String text;
}
