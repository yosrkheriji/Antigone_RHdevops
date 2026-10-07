package com.antigone.rh.ai.controller;

import com.antigone.rh.ai.dto.GenerateMediaPlanRequest;
import com.antigone.rh.ai.dto.MediaPlanGenerationResponse;
import com.antigone.rh.ai.security.AiAccessScope;
import com.antigone.rh.ai.service.AiRateLimiter;
import com.antigone.rh.ai.service.DriveProvisioningService;
import com.antigone.rh.ai.service.MediaPlanGenerationService;
import com.antigone.rh.ai.sse.AiSseFactory;
import com.antigone.rh.ai.sse.AiSseSession;
import com.antigone.rh.ai.sse.AiTaskStreamer;
import com.antigone.rh.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * Generation de media plan.
 *
 * <p>Deux representations du meme traitement : SSE pour le frontend, qui doit
 * afficher la progression d'une generation pouvant durer une minute, et JSON pour
 * les integrations et les tests, qui veulent un resultat en un appel. Le
 * {@code @PreAuthorize} ne couvre que le premier filtre ; le perimetre client, lui,
 * est verifie dans le service, marque par marque.
 */
@RestController
@RequestMapping("/api/v1/media-plans")
@RequiredArgsConstructor
@Tag(name = "Assistant - Media Plan", description = "Generation de media plan mensuel assistee par IA")
public class AiMediaPlanController {

    private final MediaPlanGenerationService generationService;
    private final DriveProvisioningService driveProvisioning;
    private final AiRateLimiter rateLimiter;
    private final AiSseFactory sseFactory;
    private final AiTaskStreamer streamer;
    private final AiAccessScope accessScope;

    /**
     * Genere le media plan en streaming.
     *
     * <p>Evenements : {@code tool_call_start} / {@code tool_call_end} pour chacune
     * des sept etapes du pipeline, {@code heartbeat} pendant les phases longues,
     * puis {@code structured_result} et {@code done}.
     */
    @PostMapping(value = "/generate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyAuthority('VIEW_MEDIA_PLAN', 'VIEW_TOUS_MEDIA_PLAN', 'ROLE_ADMIN')")
    @Operation(summary = "Genere un media plan mensuel (streaming SSE)")
    public SseEmitter generateStreaming(@RequestBody @Valid GenerateMediaPlanRequest request) {
        AuthPrincipal principal = accessScope.current();
        accessScope.requireMediaPlanAssistant(principal);
        rateLimiter.checkAndRecord(principal.getAccountId());

        AiSseSession session = sseFactory.open();
        return streamer.run(session, sse -> MediaPlanGenerationResponse.from(
                generationService.generate(principal, request.getClientId(), request.getMonth(),
                        progressListener(sse))));
    }

    /**
     * Meme generation, reponse JSON en un appel.
     *
     * <p>Necessaire pour les integrations et les tests d'integration ; le frontend,
     * lui, doit utiliser la variante SSE sous peine d'attendre jusqu'a 90 s sans
     * aucun retour visible.
     */
    @PostMapping(value = "/generate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('VIEW_MEDIA_PLAN', 'VIEW_TOUS_MEDIA_PLAN', 'ROLE_ADMIN')")
    @Operation(summary = "Genere un media plan mensuel (reponse JSON bloquante)")
    public ResponseEntity<MediaPlanGenerationResponse> generateBlocking(
            @RequestBody @Valid GenerateMediaPlanRequest request) {
        AuthPrincipal principal = accessScope.current();
        accessScope.requireMediaPlanAssistant(principal);
        rateLimiter.checkAndRecord(principal.getAccountId());

        return ResponseEntity.ok(MediaPlanGenerationResponse.from(
                generationService.generate(principal, request.getClientId(), request.getMonth(),
                        MediaPlanGenerationService.ProgressListener.NOOP)));
    }

    /**
     * Reprend l'approvisionnement Drive des lignes restees en PENDING.
     *
     * <p>Reprise ciblee : seule l'etape Drive est rejouee, la generation n'est jamais
     * relancee — une indisponibilite passagere ne doit pas couter un second appel LLM.
     */
    @PostMapping("/{clientId}/{month}/retry-drive")
    @PreAuthorize("hasAnyAuthority('VIEW_MEDIA_PLAN', 'VIEW_TOUS_MEDIA_PLAN', 'ROLE_ADMIN')")
    @Operation(summary = "Retente l'approvisionnement Drive des publications marquees PENDING")
    public ResponseEntity<Map<String, Object>> retryDrive(@PathVariable Long clientId,
                                                          @PathVariable String month) {
        AuthPrincipal principal = accessScope.current();
        accessScope.requireMediaPlanAssistant(principal);
        accessScope.requireClientAllowed(principal, clientId);

        int updated = driveProvisioning.retryPending(clientId, month);
        return ResponseEntity.ok(Map.of(
                "clientId", clientId,
                "month", month,
                "updated", updated,
                "message", updated == 0
                        ? "Aucune publication approvisionnee : Drive reste indisponible ou rien n'etait en attente."
                        : updated + " publication(s) approvisionnee(s)."));
    }

    /** Traduit la progression du pipeline en evenements SSE d'outil. */
    private MediaPlanGenerationService.ProgressListener progressListener(AiSseSession session) {
        return new MediaPlanGenerationService.ProgressListener() {
            @Override
            public void start(MediaPlanGenerationService.Step step, Object arguments) {
                session.toolCallStart(step.toolName(), arguments);
            }

            @Override
            public void end(MediaPlanGenerationService.Step step, boolean success, String detail) {
                session.toolCallEnd(step.toolName(), success, detail);
            }
        };
    }
}
