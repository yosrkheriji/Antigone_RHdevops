package com.antigone.rh.ai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Conversation de l'assistant IA. Cloisonnée par compte : {@code compteId} est la
 * clé d'isolation appliquée dans chaque requête du repository, jamais seulement
 * côté UI.
 */
@Entity
@Table(name = "ai_conversations", indexes = {
        @Index(name = "idx_ai_conv_compte", columnList = "compte_id"),
        @Index(name = "idx_ai_conv_updated", columnList = "updated_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Propriétaire — un compte ne peut charger que ses propres conversations. */
    @Column(name = "compte_id", nullable = false)
    private Long compteId;

    @Column(nullable = false, length = 200)
    private String title;

    @Builder.Default
    @Column(nullable = false)
    private Boolean pinned = false;

    /**
     * Résumé cumulatif des messages évincés de la fenêtre de contexte. Réinjecté
     * en tête du prompt pour que la conversation reste cohérente au-delà de
     * {@code app.ai.memory.max-messages}.
     */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /** Nombre de messages déjà absorbés par {@link #summary} (audit / debug). */
    @Builder.Default
    @Column(name = "summarized_message_count", nullable = false)
    private Integer summarizedMessageCount = 0;

    /** Compteur monotone servant à ordonner les messages sans dépendre du timestamp. */
    @Builder.Default
    @Column(name = "next_sequence", nullable = false)
    private Long nextSequence = 0L;

    /** Soft-delete : la conversation disparaît de l'API mais reste auditable. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
