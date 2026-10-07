package com.antigone.rh.ai.rag;

import com.antigone.rh.ai.config.AiProperties;
import com.antigone.rh.ai.entity.AiDocumentChunk;
import com.antigone.rh.ai.entity.AiSourceType;
import com.antigone.rh.ai.repository.AiDocumentChunkRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Recherche hybride dense + lexicale, fusionnee par Reciprocal Rank Fusion.
 *
 * <p>Les deux branches sont executees pour chaque requete. La branche dense capte
 * la proximite semantique (« contenu sur le lancement produit » retrouve un post
 * intitule « teasing nouvelle collection »), la branche lexicale capte les
 * correspondances exactes que les embeddings diluent : nom de marque, numero de
 * facture, date.
 *
 * <p>La fusion utilise RRF plutot qu'une somme ponderee de scores bruts, parce que
 * {@code ts_rank_cd} et la similarite cosinus vivent sur des echelles
 * incomparables, alors que leurs rangs se fusionnent directement. Le poids
 * configure module la contribution de chaque branche.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HybridRetriever {

    private final AiDocumentChunkRepository chunkRepository;
    private final AiProperties properties;
    private final PgVectorSupport pgVectorSupport;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;

    @Transactional(readOnly = true)
    public List<RagHit> search(RagQuery query) {
        if (query.text() == null || query.text().isBlank()) {
            return List.of();
        }
        AiProperties.Rag config = properties.getRag();
        int finalTopK = query.topK() != null ? query.topK() : config.getFinalTopK();

        List<Long> denseIds = searchDense(query, config.getDenseTopK());
        List<Long> sparseIds = searchSparse(query, config.getSparseTopK());

        if (denseIds.isEmpty() && sparseIds.isEmpty()) {
            return List.of();
        }

        Map<Long, RankedEntry> fused = fuse(denseIds, sparseIds, config);

        List<Long> orderedIds = fused.values().stream()
                .sorted(Comparator.comparingDouble(RankedEntry::score).reversed())
                .limit(finalTopK)
                .map(RankedEntry::id)
                .toList();

        Map<Long, AiDocumentChunk> chunks = new LinkedHashMap<>();
        chunkRepository.findAllById(orderedIds).forEach(chunk -> chunks.put(chunk.getId(), chunk));

        List<RagHit> hits = new ArrayList<>();
        for (Long id : orderedIds) {
            AiDocumentChunk chunk = chunks.get(id);
            if (chunk == null) {
                continue;
            }
            RankedEntry entry = fused.get(id);
            hits.add(new RagHit(chunk, entry.score(), entry.denseRank(), entry.sparseRank()));
        }

        log.debug("RAG hybride : {} dense + {} lexical -> {} chunks fusionnes",
                denseIds.size(), sparseIds.size(), hits.size());
        return hits;
    }

    // ---- Fusion ------------------------------------------------------------

    private Map<Long, RankedEntry> fuse(List<Long> denseIds, List<Long> sparseIds, AiProperties.Rag config) {
        double denseWeight = config.getDenseWeight();
        double sparseWeight = 1.0 - denseWeight;
        int k = config.getRrfK();

        Map<Long, RankedEntry> fused = new LinkedHashMap<>();
        for (int rank = 0; rank < denseIds.size(); rank++) {
            Long id = denseIds.get(rank);
            double contribution = denseWeight / (k + rank + 1.0);
            fused.merge(id, new RankedEntry(id, contribution, rank + 1, null), RankedEntry::mergeWith);
        }
        for (int rank = 0; rank < sparseIds.size(); rank++) {
            Long id = sparseIds.get(rank);
            double contribution = sparseWeight / (k + rank + 1.0);
            fused.merge(id, new RankedEntry(id, contribution, null, rank + 1), RankedEntry::mergeWith);
        }
        return fused;
    }

    private record RankedEntry(Long id, double score, Integer denseRank, Integer sparseRank) {

        RankedEntry mergeWith(RankedEntry other) {
            return new RankedEntry(
                    id,
                    score + other.score,
                    denseRank != null ? denseRank : other.denseRank,
                    sparseRank != null ? sparseRank : other.sparseRank);
        }
    }

    // ---- Branche lexicale --------------------------------------------------

    private List<Long> searchSparse(RagQuery query, int limit) {
        Collection<String> sourceTypes = sourceTypeNames(query);
        boolean noTypeFilter = sourceTypes.isEmpty();
        // Les termes sont combines par OU, sinon une question en langage naturel
        // n'aurait quasiment aucune chance de correspondre a un document entier.
        String expression = LexicalQuery.from(query.text());
        if (expression.isBlank()) {
            return List.of();
        }
        try {
            List<Object[]> rows = chunkRepository.searchLexical(
                    expression,
                    properties.getRag().getTextSearchConfig(),
                    query.scope().unrestricted(),
                    query.scope().clientIds(),
                    noTypeFilter,
                    noTypeFilter ? Set.of("") : sourceTypes,
                    limit);
            return rows.stream().map(row -> ((Number) row[0]).longValue()).toList();
        } catch (Exception e) {
            log.warn("Recherche lexicale en echec ({}) - la fusion se limite a la branche dense",
                    e.getMessage());
            return List.of();
        }
    }

    // ---- Branche dense -----------------------------------------------------

    private List<Long> searchDense(RagQuery query, int limit) {
        EmbeddingModel model = embeddingModelProvider.getIfAvailable();
        if (model == null) {
            return List.of();
        }
        float[] queryVector;
        try {
            Embedding embedding = model.embed(query.text()).content();
            queryVector = embedding.vector();
        } catch (Exception e) {
            log.warn("Embedding de la requete en echec ({}) - la fusion se limite a la branche lexicale",
                    e.getMessage());
            return List.of();
        }

        return pgVectorSupport.isAvailable()
                ? searchDenseNative(query, queryVector, limit)
                : searchDenseInMemory(query, queryVector, limit);
    }

    private List<Long> searchDenseNative(RagQuery query, float[] queryVector, int limit) {
        Collection<String> sourceTypes = sourceTypeNames(query);
        boolean noTypeFilter = sourceTypes.isEmpty();
        try {
            List<Object[]> rows = chunkRepository.searchDensePgVector(
                    EmbeddingCodec.encode(queryVector),
                    query.scope().unrestricted(),
                    query.scope().clientIds(),
                    noTypeFilter,
                    noTypeFilter ? Set.of("") : sourceTypes,
                    limit);
            return rows.stream().map(row -> ((Number) row[0]).longValue()).toList();
        } catch (Exception e) {
            log.warn("Recherche pgvector en echec ({}) - repli sur le calcul en Java", e.getMessage());
            return searchDenseInMemory(query, queryVector, limit);
        }
    }

    /**
     * Repli sans pgvector : le perimetre autorise est charge puis trie par cosinus.
     * Le filtre client reste applique dans la requete JPA, donc le repli n'elargit
     * jamais l'acces aux donnees.
     */
    private List<Long> searchDenseInMemory(RagQuery query, float[] queryVector, int limit) {
        Set<AiSourceType> sourceTypes = query.sourceTypes() == null ? Set.of() : query.sourceTypes();
        boolean noTypeFilter = sourceTypes.isEmpty();
        List<AiDocumentChunk> candidates = chunkRepository.findEmbeddedInScope(
                query.scope().unrestricted(),
                query.scope().clientIds(),
                noTypeFilter,
                noTypeFilter ? Set.of(AiSourceType.BRAND) : sourceTypes);

        return candidates.stream()
                .map(chunk -> Map.entry(chunk.getId(),
                        EmbeddingCodec.cosine(queryVector, EmbeddingCodec.decode(chunk.getEmbedding()))))
                .filter(entry -> entry.getValue() > 0.0)
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    private Collection<String> sourceTypeNames(RagQuery query) {
        if (query.sourceTypes() == null || query.sourceTypes().isEmpty()) {
            return Set.of();
        }
        return query.sourceTypes().stream().map(Enum::name).collect(Collectors.toSet());
    }
}
