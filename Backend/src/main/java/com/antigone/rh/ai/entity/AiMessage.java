package com.antigone.rh.ai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Message d'une conversation, tel qu'exposé par l'API et conservé pour l'audit.
 *
 * <p>Distinct de la mémoire LangChain4j ({@link AiChatMemoryRecord}) : celle-ci
 * stocke le format interne de la librairie, celui-ci le contrat stable rendu au
 * frontend. Le découplage évite qu'une montée de version de LangChain4j ne casse
 * l'historique déjà affiché aux utilisateurs.
 */
@Entity
@Table(name = "ai_messages", indexes = {
        @Index(name = "idx_ai_msg_conversation", columnList = "conversation_id, sequence")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiMessageRole role;

    @Column(columnDefinition = "TEXT")
    private String content;

    /** Appels d'outils effectués sur ce tour, sérialisés en JSON (audit + debug). */
    @Column(name = "tool_calls", columnDefinition = "TEXT")
    private String toolCalls;

    /**
     * Résultat structuré éventuel (media plan, relance, explication de paie),
     * sérialisé en JSON — c'est ce qui est renvoyé dans l'événement SSE
     * {@code structured_result} et rejoué au rechargement de la conversation.
     */
    @Column(name = "structured_result", columnDefinition = "TEXT")
    private String structuredResult;

    /** Capacité ayant produit le message : MEDIA_PLAN, REMINDER, PAYSLIP, GENERAL. */
    @Column(length = 40)
    private String capability;

    /** Ordre stable dans la conversation. */
    @Column(nullable = false)
    private Long sequence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
