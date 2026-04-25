package com.backend.demo.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Incoming request body for POST /api/v1/chat/query
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "Pesan tidak boleh kosong")
    @Size(max = 1000, message = "Pesan terlalu panjang (maks 1000 karakter)")
    private String message;

    /**
     * Conversation history (max 6 messages sent to keep prompt compact).
     * Newest-last order: [user, ai, user, ai, ...]
     */
    @Builder.Default
    private List<ChatMessage> history = new ArrayList<>();
}
