package com.antigone.rh.ai.unit;

import com.antigone.rh.ai.config.AiProperties;
import com.antigone.rh.ai.entity.AiDocumentChunk;
import com.antigone.rh.ai.entity.AiSourceType;
import com.antigone.rh.ai.rag.EmbeddingCodec;
import com.antigone.rh.ai.rag.HybridRetriever;
import com.antigone.rh.ai.rag.PgVectorSupport;
import com.antigone.rh.ai.rag.RagHit;
import com.antigone.rh.ai.rag.RagQuery;
import com.antigone.rh.ai.repository.AiDocumentChunkRepository;
import com.antigone.rh.ai.security.AiAccessScope;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Scenario 11 : les deux branches contribuent au contexte final.
 *
 * <p>Le test porte sur la fusion elle-meme — quelles branches ont ramene quoi, et
 * dans quel ordre — plutot que sur la pertinence semantique, qui depend du modele
 * d'embedding et ne serait pas deterministe.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HybridRetrieverTest {

    @Mock
    private AiDocumentChunkRepository chunkRepository;

    @Mock
    private PgVectorSupport pgVectorSupport;

    @Mock
    private EmbeddingModel embeddingModel;

    private HybridRetriever retriever;
    private AiProperties properties;

    private static final AiAccessScope.ClientScope SCOPE = AiAccessScope.ClientScope.of(List.of(1L));

    @BeforeEach
    void setUp() {
        properties = new AiProperties();

        @SuppressWarnings("unchecked")
        ObjectProvider<EmbeddingModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(embeddingModel);
        when(embeddingModel.embed(anyString()))
                .thenReturn(Response.from(Embedding.from(new float[]{1f, 0f, 0f})));

        retriever = new HybridRetriever(chunkRepository, properties, pgVectorSupport, provider);
    }

    private AiDocumentChunk chunk(long id, String content) {
        return AiDocumentChunk.builder()
                .id(id)
                .sourceType(AiSourceType.MEDIA_PLAN)
                .sourceId(id)
                .clientId(1L)
                .content(content)
                .contentHash("h" + id)
                .embedding(EmbeddingCodec.encode(new float[]{1f, 0f, 0f}))
                .build();
    }

    private void givenDenseResults(Long... ids) {
        when(pgVectorSupport.isAvailable()).thenReturn(true);
        List<Object[]> rows = java.util.Arrays.stream(ids)
                .map(id -> new Object[]{id, 0.9})
                .toList();
        when(chunkRepository.searchDensePgVector(anyString(), anyBoolean(), any(), anyBoolean(), any(), anyInt()))
                .thenReturn(rows);
    }

    private void givenSparseResults(Long... ids) {
        List<Object[]> rows = java.util.Arrays.stream(ids)
                .map(id -> new Object[]{id, 0.5})
                .toList();
        when(chunkRepository.searchLexical(anyString(), anyString(), anyBoolean(), any(),
                anyBoolean(), any(), anyInt())).thenReturn(rows);
    }

    private void givenChunks(AiDocumentChunk... chunks) {
        when(chunkRepository.findAllById(any())).thenReturn(List.of(chunks));
    }

    @Test
    @DisplayName("Un document trouve par les deux branches est marque dense+lexical")
    void documentFoundByBothBranchesIsMarkedAsSuch() {
        givenDenseResults(10L, 11L);
        givenSparseResults(11L, 12L);
        givenChunks(chunk(10L, "dense seul"), chunk(11L, "les deux"), chunk(12L, "lexical seul"));

        List<RagHit> hits = retriever.search(RagQuery.of("lancement produit Alpha", SCOPE,
                java.util.Set.of(AiSourceType.MEDIA_PLAN)));

        assertThat(hits).extracting(hit -> hit.chunk().getId())
                .containsExactlyInAnyOrder(10L, 11L, 12L);

        RagHit both = hits.stream().filter(h -> h.chunk().getId() == 11L).findFirst().orElseThrow();
        assertThat(both.fromDense()).isTrue();
        assertThat(both.fromSparse()).isTrue();
        assertThat(both.provenance()).isEqualTo("dense+lexical");
    }

    @Test
    @DisplayName("Un document trouve par une seule branche garde sa provenance")
    void singleBranchDocumentsKeepTheirProvenance() {
        givenDenseResults(10L);
        givenSparseResults(12L);
        givenChunks(chunk(10L, "dense seul"), chunk(12L, "lexical seul"));

        List<RagHit> hits = retriever.search(RagQuery.of("requete mixte", SCOPE, null));

        RagHit dense = hits.stream().filter(h -> h.chunk().getId() == 10L).findFirst().orElseThrow();
        RagHit sparse = hits.stream().filter(h -> h.chunk().getId() == 12L).findFirst().orElseThrow();

        assertThat(dense.provenance()).isEqualTo("dense");
        assertThat(dense.sparseRank()).isNull();
        assertThat(sparse.provenance()).isEqualTo("lexical");
        assertThat(sparse.denseRank()).isNull();
    }

    @Test
    @DisplayName("La fusion RRF classe en tete le document remonte par les deux branches")
    void rrfRanksTheConsensusDocumentFirst() {
        // 11 est second en dense et premier en lexical : la somme des deux
        // contributions doit le placer devant 10, premier d'une seule branche.
        givenDenseResults(10L, 11L);
        givenSparseResults(11L, 12L);
        givenChunks(chunk(10L, "a"), chunk(11L, "b"), chunk(12L, "c"));

        List<RagHit> hits = retriever.search(RagQuery.of("requete", SCOPE, null));

        assertThat(hits.get(0).chunk().getId()).isEqualTo(11L);
        assertThat(hits.get(0).fusedScore()).isGreaterThan(hits.get(1).fusedScore());
    }

    @Test
    @DisplayName("Le poids dense pilote l'arbitrage entre les deux branches")
    void denseWeightShiftsTheBalance() {
        givenDenseResults(10L);
        givenSparseResults(12L);
        givenChunks(chunk(10L, "dense"), chunk(12L, "lexical"));

        properties.getRag().setDenseWeight(0.9);
        assertThat(retriever.search(RagQuery.of("q", SCOPE, null)).get(0).chunk().getId()).isEqualTo(10L);

        properties.getRag().setDenseWeight(0.1);
        assertThat(retriever.search(RagQuery.of("q", SCOPE, null)).get(0).chunk().getId()).isEqualTo(12L);
    }

    @Test
    @DisplayName("Une branche lexicale en echec laisse la branche dense repondre")
    void lexicalFailureDegradesGracefully() {
        givenDenseResults(10L);
        when(chunkRepository.searchLexical(anyString(), anyString(), anyBoolean(), any(),
                anyBoolean(), any(), anyInt())).thenThrow(new RuntimeException("tsvector indisponible"));
        givenChunks(chunk(10L, "dense"));

        List<RagHit> hits = retriever.search(RagQuery.of("q", SCOPE, null));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).provenance()).isEqualTo("dense");
    }

    @Test
    @DisplayName("Sans pgvector, le repli en Java trie bien par cosinus")
    void javaFallbackRanksByCosine() {
        when(pgVectorSupport.isAvailable()).thenReturn(false);
        givenSparseResults();

        AiDocumentChunk aligned = chunk(20L, "aligne");
        AiDocumentChunk orthogonal = chunk(21L, "orthogonal");
        orthogonal.setEmbedding(EmbeddingCodec.encode(new float[]{0f, 1f, 0f}));
        AiDocumentChunk partial = chunk(22L, "partiel");
        partial.setEmbedding(EmbeddingCodec.encode(new float[]{0.7f, 0.7f, 0f}));

        when(chunkRepository.findEmbeddedInScope(anyBoolean(), any(), anyBoolean(), any()))
                .thenReturn(List.of(orthogonal, partial, aligned));
        givenChunks(aligned, partial);

        List<RagHit> hits = retriever.search(RagQuery.of("q", SCOPE, null));

        // Le vecteur orthogonal a un cosinus nul et sort du resultat.
        assertThat(hits).extracting(hit -> hit.chunk().getId()).containsExactly(20L, 22L);
    }

    @Test
    @DisplayName("Une requete vide ne declenche aucune recherche")
    void blankQueryReturnsNothing() {
        assertThat(retriever.search(RagQuery.of("   ", SCOPE, null))).isEmpty();
        assertThat(retriever.search(RagQuery.of(null, SCOPE, null))).isEmpty();
    }

    @Test
    @DisplayName("Le nombre de resultats est borne par final-top-k")
    void resultsAreCappedByFinalTopK() {
        properties.getRag().setFinalTopK(2);
        givenDenseResults(10L, 11L, 12L);
        givenSparseResults(13L);
        givenChunks(chunk(10L, "a"), chunk(11L, "b"), chunk(12L, "c"), chunk(13L, "d"));

        assertThat(retriever.search(RagQuery.of("q", SCOPE, null))).hasSize(2);
    }
}
