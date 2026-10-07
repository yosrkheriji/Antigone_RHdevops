package com.antigone.rh.ai.controller;

import com.antigone.rh.ai.dto.GenerateReminderRequest;
import com.antigone.rh.ai.dto.ReminderResponse;
import com.antigone.rh.ai.security.AiAccessScope;
import com.antigone.rh.ai.service.AiRateLimiter;
import com.antigone.rh.ai.service.ReminderGenerationService;
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

/**
 * Relances clients, reservees aux administrateurs.
 *
 * <p>Deux etapes distinctes : {@code /generate} redige un brouillon et le persiste,
 * {@code /{id}/send} l'expedie. L'assistant ne fait jamais partir un courrier de
 * lui-meme — la validation humaine est le clic de l'utilisateur sur un texte qu'il
 * vient de lire.
 */
@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
@Tag(name = "Assistant - Relances", description = "Redaction et envoi de relances de factures impayees")
public class AiReminderController {

    private final ReminderGenerationService reminderService;
    private final AiRateLimiter rateLimiter;
    private final AiSseFactory sseFactory;
    private final AiTaskStreamer streamer;
    private final AiAccessScope accessScope;

    @PostMapping(value = "/generate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'VIEW_FINANCE')")
    @Operation(summary = "Redige un brouillon de relance (streaming SSE). N'envoie rien.")
    public SseEmitter generateStreaming(@RequestBody @Valid GenerateReminderRequest request) {
        AuthPrincipal principal = accessScope.current();
        accessScope.requireReminderAssistant(principal);
        rateLimiter.checkAndRecord(principal.getAccountId());

        AiSseSession session = sseFactory.open();
        return streamer.run(session, sse -> {
            sse.toolCallStart("InvoiceLookupTool", java.util.Map.of("invoiceId", request.getInvoiceId()));
            ReminderResponse response =
                    ReminderResponse.from(reminderService.generate(principal, request.getInvoiceId()));
            sse.toolCallEnd("InvoiceLookupTool", true, response.getTone());
            return response;
        });
    }

    @PostMapping(value = "/generate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'VIEW_FINANCE')")
    @Operation(summary = "Redige un brouillon de relance (reponse JSON bloquante). N'envoie rien.")
    public ResponseEntity<ReminderResponse> generateBlocking(
            @RequestBody @Valid GenerateReminderRequest request) {
        AuthPrincipal principal = accessScope.current();
        accessScope.requireReminderAssistant(principal);
        rateLimiter.checkAndRecord(principal.getAccountId());

        return ResponseEntity.ok(
                ReminderResponse.from(reminderService.generate(principal, request.getInvoiceId())));
    }

    /**
     * Expedie un brouillon relu et valide par l'utilisateur.
     *
     * <p>Envoie exactement le texte enregistre, sans regeneration : l'utilisateur
     * doit recevoir ce qu'il a lu. Un second appel sur une relance deja partie ne la
     * renvoie pas — le client recevrait deux fois le meme rappel.
     *
     * <p>Pas de quota IA ici : aucune generation n'a lieu, seul un envoi d'email.
     */
    @PostMapping(value = "/{reminderId}/send", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'VIEW_FINANCE')")
    @Operation(summary = "Envoie au client le brouillon de relance valide par l'utilisateur")
    public ResponseEntity<ReminderResponse> send(@PathVariable Long reminderId) {
        AuthPrincipal principal = accessScope.current();
        accessScope.requireReminderAssistant(principal);

        return ResponseEntity.ok(ReminderResponse.from(reminderService.send(principal, reminderId)));
    }
}
