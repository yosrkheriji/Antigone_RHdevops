package com.antigone.rh.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Entree de la liste des conversations. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryDTO {
    private Long id;
    private String title;
    private Boolean pinned;
    private Long messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
