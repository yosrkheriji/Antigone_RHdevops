package com.antigone.rh.ai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Chunk indexé pour le RAG hybride.
 *
 * <p>{@code clientId} porte l'isolation inter-marques : toute recherche le filtre
 * en SQL, de sorte qu'un utilisateur ne peut jamais remonter le contexte d'une
 * marque hors de son périmètre — même si le prompt le lui demande.
 *
 * <p>L'embedding est stocké en TEXT (tableau JSON de flottants) afin de rester
 * portable sur un Postgres sans pgvector. Quand l'extension est disponible, une
 * colonne {@code embedding_vec} de type {@code vector} est ajoutée et peuplée en
 * parallèle par {@code PgVectorSupport} : la recherche dense bascule alors sur
 * l'opérateur natif {@code <=>}.
 */
@Entity
@Table(name = "ai_document_chunks", indexes = {
        @Index(name = "idx_ai_chunk_source", columnList = "source_type, source_id"),
        @Index(name = "idx_ai_chunk_client", columnList = "client_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiDocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private AiSourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    /** Marque concernée. Null = document transverse (non rattaché à un client). */
    @Column(name = "client_id")
    private Long clientId;

    /** Mois de rattachement au format "YYYY-MM", pour filtrer l'historique. */
    @Column(name = "period_month", length = 7)
    private String periodMonth;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Tableau JSON de flottants. Null tant que l'embedding n'a pas été calculé. */
    @Column(columnDefinition = "TEXT")
    private String embedding;

    /** Détecte les changements pour ne réembedder que ce qui a bougé. */
    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
