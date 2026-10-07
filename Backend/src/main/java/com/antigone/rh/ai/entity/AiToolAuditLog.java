package com.antigone.rh.ai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Piste d'audit des appels d'outils. Chaque {@code @Tool} valide le rôle de
 * l'appelant puis journalise ici — indispensable pour prouver qu'aucun accès
 * hors périmètre n'a eu lieu, y compris sur tentative de prompt injection.
 */
@Entity
@Table(name = "ai_tool_audit", indexes = {
        @Index(name = "idx_ai_tool_audit_compte", columnList = "compte_id, created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiToolAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "compte_id")
    private Long compteId;

    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(name = "tool_name", nullable = false, length = 80)
    private String toolName;

    @Column(columnDefinition = "TEXT")
    private String arguments;

    /** GRANTED, DENIED ou ERROR. */
    @Column(nullable = false, length = 20)
    private String outcome;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
