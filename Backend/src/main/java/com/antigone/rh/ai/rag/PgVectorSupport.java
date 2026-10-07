package com.antigone.rh.ai.rag;

import com.antigone.rh.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Prépare le schéma de recherche du RAG hybride et détermine si pgvector est
 * exploitable sur l'instance Postgres courante.
 *
 * <p>La recherche lexicale repose sur {@code to_tsvector}, présent dans tout
 * Postgres : son index GIN est donc toujours créé. La recherche dense privilégie
 * l'opérateur natif {@code <=>} de pgvector ; quand l'extension est absente ou non
 * installable (droits insuffisants sur une instance managée), on bascule sur un
 * calcul de cosinus en Java. La fonctionnalité reste identique, seule la
 * performance change — et le développement local ne dépend plus d'une extension.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PgVectorSupport {

    private final JdbcTemplate jdbcTemplate;
    private final AiProperties properties;

    private volatile boolean available = false;
    private volatile int columnDimensions = -1;

    public boolean isAvailable() {
        return available;
    }

    /**
     * Dimension declaree sur la colonne {@code embedding_vec}, ou -1 si pgvector est
     * indisponible. Un vecteur d'une autre taille est rejete par Postgres, et
     * l'erreur marque la transaction entiere comme rollback-only : mieux vaut
     * comparer avant d'ecrire que rattraper apres.
     */
    public int columnDimensions() {
        return columnDimensions;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(50)
    public void initialise() {
        createLexicalIndex();
        available = tryEnablePgVector();
        log.info("RAG hybride — branche lexicale : tsvector ({}), branche dense : {}",
                properties.getRag().getTextSearchConfig(),
                available ? "pgvector natif" : "cosinus calculé en Java (pgvector indisponible)");
    }

    private void createLexicalIndex() {
        String config = sanitizeConfig(properties.getRag().getTextSearchConfig());
        try {
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_ai_chunk_fts ON ai_document_chunks "
                            + "USING GIN (to_tsvector('" + config + "', content))");
        } catch (Exception e) {
            log.warn("Index plein-texte non créé ({}) — la recherche lexicale reste fonctionnelle "
                    + "mais sans index", e.getMessage());
        }
    }

    private boolean tryEnablePgVector() {
        int dimensions = properties.getEmbedding().getDimensions();
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        } catch (Exception e) {
            log.info("Extension pgvector indisponible ({}) — repli sur le calcul en Java", e.getMessage());
            return false;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE ai_document_chunks ADD COLUMN IF NOT EXISTS embedding_vec vector("
                    + dimensions + ")");
            // HNSW donne de bien meilleurs rappels qu'ivfflat sans nécessiter de
            // phase d'entraînement — adapté à un index qui grossit en continu.
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_chunk_vec ON ai_document_chunks "
                    + "USING hnsw (embedding_vec vector_cosine_ops)");
            columnDimensions = dimensions;
            return true;
        } catch (Exception e) {
            log.warn("pgvector présent mais colonne/index non créés ({}) — repli sur le calcul en Java",
                    e.getMessage());
            return false;
        }
    }

    /**
     * La configuration textuelle est interpolée dans un DDL (un paramètre lié n'est
     * pas accepté à cet endroit), donc restreinte à un identifiant simple.
     */
    private String sanitizeConfig(String config) {
        if (config == null || !config.matches("[a-z_]{1,32}")) {
            log.warn("Configuration de recherche textuelle invalide ({}) — repli sur 'simple'", config);
            return "simple";
        }
        return config;
    }
}
