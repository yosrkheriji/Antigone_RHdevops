package com.antigone.rh.ai.controller;

import com.antigone.rh.ai.dto.ConversationDetailDTO;
import com.antigone.rh.ai.dto.ConversationSummaryDTO;
import com.antigone.rh.ai.dto.CreateConversationRequest;
import com.antigone.rh.ai.dto.SendMessageRequest;
import com.antigone.rh.ai.dto.UpdateConversationRequest;
import com.antigone.rh.ai.security.AiAccessScope;
import com.antigone.rh.ai.service.ChatOrchestratorService;
import com.antigone.rh.ai.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Conversations de l'assistant.
 *
 * <p>Aucun identifiant d'utilisateur ne transite par l'API : le proprietaire est
 * toujours celui du JWT. Une conversation appartenant a un autre compte repond 404.
 */
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Tag(name = "Assistant - Conversations",
        description = "CRUD des conversations et envoi de messages en streaming SSE")
public class ConversationController {

    private final ConversationService conversationService;
    private final ChatOrchestratorService orchestrator;
    private final AiAccessScope accessScope;

    @GetMapping
    @Operation(summary = "Liste les conversations du compte connecte, triees par date de mise a jour")
    public ResponseEntity<Page<ConversationSummaryDTO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(direction, sort));
        return ResponseEntity.ok(conversationService.list(accessScope.current(), pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detail d'une conversation avec son historique complet de messages")
    public ResponseEntity<ConversationDetailDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(conversationService.get(accessScope.current(), id));
    }

    @PostMapping
    @Operation(summary = "Cree une conversation ; le titre est genere au premier message si absent")
    public ResponseEntity<ConversationSummaryDTO> create(
            @RequestBody(required = false) @Valid CreateConversationRequest request) {
        return ResponseEntity.ok(conversationService.create(accessScope.current(), request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Renomme ou epingle une conversation")
    public ResponseEntity<ConversationSummaryDTO> update(@PathVariable Long id,
                                                        @RequestBody @Valid UpdateConversationRequest request) {
        return ResponseEntity.ok(conversationService.update(accessScope.current(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime une conversation (soft-delete, purge de la memoire LLM)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        conversationService.delete(accessScope.current(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Envoie un message et rend la reponse en streaming.
     *
     * <p>Requete en JSON, reponse en {@code text/event-stream}. {@code EventSource}
     * natif ne sachant pas emettre de POST, le frontend consomme ce flux avec
     * {@code fetch} + lecture incrementale du corps — voir API_DOCUMENTATION.md.
     */
    @PostMapping(value = "/{id}/messages",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Envoie un message ; reponse en SSE (token, tool_call_start/end, "
            + "heartbeat, structured_result, error, done)")
    public SseEmitter sendMessage(@PathVariable Long id, @RequestBody @Valid SendMessageRequest request) {
        return orchestrator.stream(accessScope.current(), id, request.getContent());
    }
}
