package com.antigone.rh.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Conversation avec son historique complet. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDetailDTO {
    private Long id;
    private String title;
    private Boolean pinned;
    private String summary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MessageDTO> messages;
}
