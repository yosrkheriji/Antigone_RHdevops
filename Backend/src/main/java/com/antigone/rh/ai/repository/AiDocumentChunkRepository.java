package com.antigone.rh.ai.repository;

import com.antigone.rh.ai.entity.AiDocumentChunk;
import com.antigone.rh.ai.entity.AiSourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Accès aux chunks du RAG hybride.
 *
 * <p>Les deux branches de recherche filtrent le périmètre client <em>dans le
 * SQL</em> ({@code unrestricted} + {@code clientIds}). Un utilisateur limité à la
 * marque A ne peut donc pas remonter de contexte de la marque B, quelle que soit
 * la formulation de son prompt.
 *
 * <p>{@code clientIds} ne doit jamais être vide : l'appelant passe un sentinelle
 * {@code -1} quand aucun client n'est autorisé, sinon Postgres reçoit un
 * {@code IN ()} syntaxiquement invalide.
 */
@Repository
public interface AiDocumentChunkRepository extends JpaRepository<AiDocumentChunk, Long> {

    List<AiDocumentChunk> findBySourceTypeAndSourceId(AiSourceType sourceType, Long sourceId);

    Optional<AiDocumentChunk> findFirstBySourceTypeAndSourceIdOrderByIdAsc(AiSourceType sourceType, Long sourceId);

    @Modifying
    void deleteBySourceTypeAndSourceId(AiSourceType sourceType, Long sourceId);

    long countByEmbeddingIsNull();

    List<AiDocumentChunk> findTop200ByEmbeddingIsNull();

    /**
     * Branche lexicale (BM25-like) : {@code ts_rank_cd} sur un {@code tsvector}
     * Postgres. Indispensable pour les correspondances exactes que la recherche
     * sémantique rate — nom de marque, référence de facture, date.
     */
    @Query(nativeQuery = true, value = """
            SELECT c.id,
                   ts_rank_cd(to_tsvector(CAST(:config AS regconfig), c.content),
                              websearch_to_tsquery(CAST(:config AS regconfig), :queryText)) AS score
            FROM ai_document_chunks c
            WHERE (:unrestricted = TRUE OR c.client_id IN (:clientIds))
              AND (:sourceTypesEmpty = TRUE OR c.source_type IN (:sourceTypes))
              AND to_tsvector(CAST(:config AS regconfig), c.content)
                  @@ websearch_to_tsquery(CAST(:config AS regconfig), :queryText)
            ORDER BY score DESC
            LIMIT :limit
            """)
    List<Object[]> searchLexical(@Param("queryText") String queryText,
                                 @Param("config") String config,
                                 @Param("unrestricted") boolean unrestricted,
                                 @Param("clientIds") Collection<Long> clientIds,
                                 @Param("sourceTypesEmpty") boolean sourceTypesEmpty,
                                 @Param("sourceTypes") Collection<String> sourceTypes,
                                 @Param("limit") int limit);

    /**
     * Branche dense native pgvector. Appelée uniquement quand
     * {@code PgVectorSupport#isAvailable()} est vrai — la colonne
     * {@code embedding_vec} et le type {@code vector} n'existent pas autrement.
     */
    @Query(nativeQuery = true, value = """
            SELECT c.id,
                   1 - (c.embedding_vec <=> CAST(:vector AS vector)) AS score
            FROM ai_document_chunks c
            WHERE c.embedding_vec IS NOT NULL
              AND (:unrestricted = TRUE OR c.client_id IN (:clientIds))
              AND (:sourceTypesEmpty = TRUE OR c.source_type IN (:sourceTypes))
            ORDER BY c.embedding_vec <=> CAST(:vector AS vector)
            LIMIT :limit
            """)
    List<Object[]> searchDensePgVector(@Param("vector") String vector,
                                       @Param("unrestricted") boolean unrestricted,
                                       @Param("clientIds") Collection<Long> clientIds,
                                       @Param("sourceTypesEmpty") boolean sourceTypesEmpty,
                                       @Param("sourceTypes") Collection<String> sourceTypes,
                                       @Param("limit") int limit);

    /**
     * Repli sans pgvector : on charge le périmètre autorisé et la similarité
     * cosinus est calculée en Java. Volumétrie assumée — quelques milliers de
     * chunks au maximum sur un périmètre client donné.
     */
    @Query("""
            SELECT c FROM AiDocumentChunk c
            WHERE c.embedding IS NOT NULL
              AND (:unrestricted = TRUE OR c.clientId IN :clientIds)
              AND (:sourceTypesEmpty = TRUE OR c.sourceType IN :sourceTypes)
            """)
    List<AiDocumentChunk> findEmbeddedInScope(@Param("unrestricted") boolean unrestricted,
                                              @Param("clientIds") Collection<Long> clientIds,
                                              @Param("sourceTypesEmpty") boolean sourceTypesEmpty,
                                              @Param("sourceTypes") Collection<AiSourceType> sourceTypes);

    /** Mise à jour de la colonne pgvector, séparée du mapping JPA portable. */
    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE ai_document_chunks
            SET embedding_vec = CAST(:vector AS vector)
            WHERE id = :id
            """)
    void updateEmbeddingVector(@Param("id") Long id, @Param("vector") String vector);
}
