package com.antigone.rh.ai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Mémoire LangChain4j persistée. Une ligne par conversation, contenant la liste
 * de {@code ChatMessage} sérialisée par {@code ChatMessageSerializer}.
 *
 * <p>Persistée en base et non en mémoire volatile : la conversation survit à un
 * redémarrage du service.
 */
@Entity
@Table(name = "ai_chat_memory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatMemoryRecord {

    /** Identifiant de mémoire = identifiant de conversation. */
    @Id
    @Column(name = "memory_id")
    private Long memoryId;

    @Column(name = "messages_json", columnDefinition = "TEXT")
    private String messagesJson;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
