package com.antigone.rh.ai.service;

import com.antigone.rh.ai.agent.AgentDefinitions;
import com.antigone.rh.ai.agent.AgentFactory;
import com.antigone.rh.ai.agent.Capability;
import com.antigone.rh.ai.config.AiAsyncConfig;
import com.antigone.rh.ai.entity.AiConversation;
import com.antigone.rh.ai.entity.AiMessageRole;
import com.antigone.rh.ai.exception.AiUnavailableException;
import com.antigone.rh.ai.memory.ConversationMemoryService;
import com.antigone.rh.ai.security.AiAccessScope;
import com.antigone.rh.ai.sse.AiSseFactory;
import com.antigone.rh.ai.sse.AiSseSession;
import com.antigone.rh.ai.tools.AiCallContext;
import com.antigone.rh.security.AuthPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestration d'un tour de conversation, du message recu au flux SSE.
 *
 * <p>Assistant unique cote utilisateur : l'intention est classee, puis le prompt et
 * le jeu d'outils correspondants sont montes. L'utilisateur ne choisit jamais un
 * « mode », et un refus d'acces se produit au niveau de l'outil, pas du routage —
 * router vers GENERAL au lieu de PAYSLIP ne contournerait donc rien.
 *
 * <p>La generation est soumise a un pool dedie : une reponse peut mobiliser une
 * minute, et les threads Tomcat doivent rester disponibles pour le login et le CRUD.
 */
@Service
@Slf4j
public class ChatOrchestratorService {

    private final ConversationService conversationService;
    private final ConversationMemoryService memoryService;
    private final AiRateLimiter rateLimiter;
    private final AiSseFactory sseFactory;
    private final AiAccessScope accessScope;
    private final ObjectMapper objectMapper;
    private final ThreadPoolTaskExecutor executor;
    private final ObjectProvider<AgentFactory> agentFactoryProvider;
    private final ObjectProvider<AgentDefinitions.IntentClassifier> classifierProvider;

    public ChatOrchestratorService(ConversationService conversationService,
                                   ConversationMemoryService memoryService,
                                   AiRateLimiter rateLimiter,
                                   AiSseFactory sseFactory,
                                   AiAccessScope accessScope,
                                   ObjectMapper objectMapper,
                                   @Qualifier(AiAsyncConfig.AI_EXECUTOR) ThreadPoolTaskExecutor executor,
                                   ObjectProvider<AgentFactory> agentFactoryProvider,
                                   ObjectProvider<AgentDefinitions.IntentClassifier> classifierProvider) {
        this.conversationService = conversationService;
        this.memoryService = memoryService;
        this.rateLimiter = rateLimiter;
        this.sseFactory = sseFactory;
        this.accessScope = accessScope;
        this.objectMapper = objectMapper;
        this.executor = executor;
        this.agentFactoryProvider = agentFactoryProvider;
        this.classifierProvider = classifierProvider;
    }

    /**
     * Traite un message et rend le flux SSE.
     *
     * <p>Les verifications synchrones (propriete de la conversation, quota,
     * disponibilite du modele) ont lieu avant l'ouverture du flux : un refus doit
     * arriver comme un code HTTP franc, pas comme un evenement {@code error} dans un
     * flux qui vient de s'ouvrir.
     */
    public SseEmitter stream(AuthPrincipal principal, Long conversationId, String message) {
        AiConversation conversation = conversationService.requireOwned(principal, conversationId);
        rateLimiter.checkAndRecord(principal.getAccountId());

        AgentFactory agentFactory = agentFactoryProvider.getIfAvailable();
        if (agentFactory == null) {
            throw new AiUnavailableException("L'assistant IA n'est pas configure (cle API manquante).");
        }

        conversationService.append(conversationId, AiMessageRole.USER, message, null, null, null);
        conversationService.ensureTitle(conversationId, message);

        AiSseSession session = sseFactory.open();
        try {
            executor.execute(() -> runGeneration(session, principal, conversation.getId(), message, agentFactory));
        } catch (Exception e) {
            // File saturee : le client recoit une erreur immediate plutot qu'un flux
            // ouvert sur une generation qui ne demarrera pas.
            log.warn("Generation refusee (pool sature) : {}", e.getMessage());
            session.error("AI_BUSY", "L'assistant traite trop de demandes. Reessayez dans un instant.");
            session.complete();
        }
        return session.emitter();
    }

    private void runGeneration(AiSseSession session, AuthPrincipal principal, Long conversationId,
                               String message, AgentFactory agentFactory) {
        Capability capability = classify(message, principal);
        AiCallContext context = AiCallContext.of(principal, conversationId);

        StringBuilder answer = new StringBuilder();
        List<Map<String, Object>> toolCalls = new ArrayList<>();
        AtomicBoolean settled = new AtomicBoolean(false);

        try {
            AgentDefinitions.ConversationalAgent agent =
                    agentFactory.conversationalAgent(capability, context);
            TokenStream stream = agent.chat(conversationId, message);

            stream.onPartialResponse(delta -> {
                        answer.append(delta);
                        session.token(delta);
                    })
                    .beforeToolExecution(before -> {
                        String tool = before.request().name();
                        session.toolCallStart(tool, parseArguments(before.request().arguments()));
                    })
                    .onToolExecuted(execution -> {
                        String tool = execution.request().name();
                        boolean success = !execution.hasFailed();
                        session.toolCallEnd(tool, success, null);
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("tool", tool);
                        entry.put("args", execution.request().arguments());
                        entry.put("status", success ? "success" : "error");
                        toolCalls.add(entry);
                    })
                    .onCompleteResponse(response -> {
                        if (!settled.compareAndSet(false, true)) {
                            return;
                        }
                        String text = response.aiMessage() != null && response.aiMessage().text() != null
                                ? response.aiMessage().text()
                                : answer.toString();
                        finish(session, conversationId, capability, text, toolCalls, context.structuredResult().get());
                    })
                    .onError(error -> {
                        if (!settled.compareAndSet(false, true)) {
                            return;
                        }
                        log.error("Generation en echec sur la conversation {} : {}",
                                conversationId, error.getMessage(), error);
                        session.error("GENERATION_FAILED",
                                "La generation a echoue : " + safeMessage(error));
                        // Le texte deja produit est conserve : l'utilisateur voit ce qui a
                        // ete genere avant l'incident plutot qu'une conversation amputee.
                        if (!answer.isEmpty()) {
                            persistQuietly(conversationId, capability, answer.toString(), toolCalls);
                        }
                        session.complete();
                    })
                    .start();
        } catch (Exception e) {
            if (settled.compareAndSet(false, true)) {
                log.error("Demarrage de la generation impossible sur la conversation {} : {}",
                        conversationId, e.getMessage(), e);
                session.error("GENERATION_FAILED", safeMessage(e));
                session.complete();
            }
        }
    }

    /**
     * Cloture : persistance d'abord, emission ensuite.
     *
     * <p>L'ordre compte. Le cahier des charges exige qu'une deconnexion client en
     * cours de generation ne fasse pas perdre le message : il est donc ecrit en base
     * avant toute tentative d'emission, laquelle peut echouer silencieusement si le
     * client est parti.
     *
     * <p>{@code structuredResult} vient du {@link AiCallContext} du tour : un outil
     * comme {@code EmailDraftTool} l'y depose pendant son execution. S'il est present,
     * il est persiste au meme titre que le texte (un rechargement de conversation doit
     * pouvoir reafficher le bouton d'envoi), puis emis en {@code structured_result} —
     * c'est le seul evenement que le widget sait rendre en composant riche.
     */
    private void finish(AiSseSession session, Long conversationId, Capability capability,
                        String text, List<Map<String, Object>> toolCalls, Object structuredResult) {
        persistQuietly(conversationId, capability, text, toolCalls, structuredResult);
        if (structuredResult != null) {
            session.structuredResult(structuredResult);
        }
        try {
            memoryService.maybeSummarize(conversationId);
        } catch (Exception e) {
            log.warn("Resume de la conversation {} en echec : {}", conversationId, e.getMessage());
        }
        session.complete();
    }

    private void persistQuietly(Long conversationId, Capability capability, String text,
                                List<Map<String, Object>> toolCalls) {
        persistQuietly(conversationId, capability, text, toolCalls, null);
    }

    private void persistQuietly(Long conversationId, Capability capability, String text,
                                List<Map<String, Object>> toolCalls, Object structuredResult) {
        try {
            conversationService.append(conversationId, AiMessageRole.ASSISTANT, text,
                    toolCalls.isEmpty() ? null : toJson(toolCalls),
                    structuredResult == null ? null : toJson(structuredResult),
                    capability.name());
        } catch (Exception e) {
            log.error("Persistance du message de la conversation {} en echec : {}",
                    conversationId, e.getMessage(), e);
        }
    }

    /**
     * Classement de l'intention. Toute defaillance retombe sur GENERAL : un routage
     * imparfait degrade la pertinence, il n'ouvre aucun acces — les outils
     * revalident systematiquement le perimetre.
     */
    private Capability classify(String message, AuthPrincipal principal) {
        AgentDefinitions.IntentClassifier classifier = classifierProvider.getIfAvailable();
        if (classifier == null) {
            return Capability.GENERAL;
        }
        Capability capability;
        try {
            capability = classifier.classify(message);
        } catch (Exception e) {
            log.debug("Classification d'intention en echec ({}) - repli sur GENERAL", e.getMessage());
            return Capability.GENERAL;
        }
        if (capability == null) {
            return Capability.GENERAL;
        }
        // Router vers une capacite a laquelle l'utilisateur n'a pas droit lui
        // presenterait des outils qui refuseront tous : autant rester generaliste et
        // repondre de maniere utile.
        boolean allowed = switch (capability) {
            case MEDIA_PLAN -> accessScope.canUseMediaPlanAssistant(principal);
            case REMINDER -> accessScope.canUseReminderAssistant(principal);
            case PAYSLIP -> accessScope.canUsePayslipAssistant(principal);
            case GENERAL -> true;
        };
        if (!allowed) {
            log.debug("Intention {} hors perimetre du compte {} - repli sur GENERAL",
                    capability, principal.getAccountId());
            return Capability.GENERAL;
        }
        return capability;
    }

    private Object parseArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(arguments, Map.class);
        } catch (Exception e) {
            return Map.of("raw", arguments);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String safeMessage(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return message.length() <= 300 ? message : message.substring(0, 300) + "...";
    }
}
