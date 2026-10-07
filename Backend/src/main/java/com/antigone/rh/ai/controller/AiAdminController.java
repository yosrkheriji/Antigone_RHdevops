package com.antigone.rh.ai.controller;

import com.antigone.rh.ai.config.AiProperties;
import com.antigone.rh.ai.rag.EmbeddingIndexService;
import com.antigone.rh.ai.rag.HybridRetriever;
import com.antigone.rh.ai.rag.PgVectorSupport;
import com.antigone.rh.ai.rag.RagHit;
import com.antigone.rh.ai.rag.RagQuery;
import com.antigone.rh.ai.security.AiAccessScope;
import com.antigone.rh.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exploitation de l'assistant : etat, reindexation, inspection du RAG.
 *
 * <p>{@code /rag/search} expose le detail de la fusion (rang dense, rang lexical,
 * score fusionne). C'est l'outil qui rend la recherche hybride verifiable plutot
 * que declarative : on voit, requete par requete, laquelle des deux branches a
 * ramene un document.
 */
@RestController
@RequestMapping("/api/v1/ai/admin")
@RequiredArgsConstructor
@Tag(name = "Assistant - Administration", description = "Etat, reindexation et inspection du RAG")
public class AiAdminController {

    private final EmbeddingIndexService indexService;
    private final HybridRetriever hybridRetriever;
    private final PgVectorSupport pgVectorSupport;
    private final AiAccessScope accessScope;
    private final AiProperties properties;
    private final ObjectProvider<dev.langchain4j.model.chat.StreamingChatModel> streamingModelProvider;
    private final ObjectProvider<dev.langchain4j.model.embedding.EmbeddingModel> embeddingModelProvider;

    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Etat de l'assistant : modeles, mode de recherche dense, configuration")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", properties.isEnabled());
        status.put("chatModelReady", streamingModelProvider.getIfAvailable() != null);
        status.put("embeddingModelReady", embeddingModelProvider.getIfAvailable() != null);
        status.put("chatModel", properties.getChat().getModel());
        status.put("embeddingModel", properties.getEmbedding().getModel());
        status.put("denseSearch", pgVectorSupport.isAvailable() ? "pgvector" : "java-cosine-fallback");
        status.put("textSearchConfig", properties.getRag().getTextSearchConfig());
        status.put("denseWeight", properties.getRag().getDenseWeight());
        status.put("memoryWindow", properties.getMemory().getMaxMessages());
        status.put("reminderTones", java.util.Map.of(
                "softMaxDays", properties.getReminder().getSoftMaxDays(),
                "firmMaxDays", properties.getReminder().getFirmMaxDays()));
        return ResponseEntity.ok(status);
    }

    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reindexe marques, projets, media plans et reglement interieur dans l'index RAG")
    public ResponseEntity<Map<String, Object>> reindex() {
        EmbeddingIndexService.IndexReport report = indexService.reindexAll();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("written", report.written);
        body.put("unchanged", report.unchanged);
        body.put("embedded", report.embedded);
        body.put("errors", report.errors);
        body.put("durationMs", report.durationMs);
        // Chaque etape (marques, projets, media plans, reglement, embeddings) est
        // isolee : un incident sur l'une ne bloque plus les autres, mais reste
        // signale ici plutot que seulement dans les logs serveur.
        body.put("errorMessages", report.errorMessages);
        return ResponseEntity.ok(body);
    }

    /**
     * Inspection de la recherche hybride, dans le perimetre de l'appelant.
     *
     * <p>Le perimetre applique est celui du compte connecte, pas un perimetre
     * d'administration : cet endpoint sert a diagnostiquer la pertinence, jamais a
     * contourner le cloisonnement.
     */
    @GetMapping("/rag/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Execute une recherche hybride et detaille la contribution de chaque branche")
    public ResponseEntity<Map<String, Object>> searchRag(@RequestParam String q,
                                                         @RequestParam(required = false) Integer topK) {
        AuthPrincipal principal = accessScope.current();
        List<RagHit> hits = hybridRetriever.search(new RagQuery(
                q, accessScope.mediaPlanClientScope(principal), null, topK));

        List<Map<String, Object>> results = hits.stream().map(hit -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("chunkId", hit.chunk().getId());
            row.put("sourceType", hit.chunk().getSourceType().name());
            row.put("sourceId", hit.chunk().getSourceId());
            row.put("clientId", hit.chunk().getClientId());
            row.put("provenance", hit.provenance());
            row.put("denseRank", hit.denseRank());
            row.put("sparseRank", hit.sparseRank());
            row.put("fusedScore", hit.fusedScore());
            row.put("excerpt", excerpt(hit.chunk().getContent()));
            return row;
        }).toList();

        return ResponseEntity.ok(Map.of(
                "query", q,
                "denseSearch", pgVectorSupport.isAvailable() ? "pgvector" : "java-cosine-fallback",
                "count", results.size(),
                "results", results));
    }

    private String excerpt(String content) {
        if (content == null) {
            return "";
        }
        String flat = content.replace('\n', ' ');
        return flat.length() <= 240 ? flat : flat.substring(0, 240) + "...";
    }
}
