package com.antigone.rh.ai.agent;

import com.antigone.rh.ai.config.AiEnabledCondition;
import com.antigone.rh.ai.config.AiProperties;
import com.antigone.rh.ai.memory.ConversationMemoryService;
import com.antigone.rh.ai.tools.AiCallContext;
import com.antigone.rh.ai.tools.AiToolFactory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/**
 * Assemble les {@code AiServices} : modele, memoire, outils, prompt systeme.
 *
 * <p>Les agents conversationnels sont construits <em>par requete</em>, pas une fois
 * pour toutes. La raison est de securite : les outils portent l'identite de leur
 * appelant, et un agent partage impliquerait de propager cette identite jusqu'au
 * thread de callback de LangChain4j — un mecanisme fragile dont l'echec ferait fuir
 * un perimetre d'un utilisateur vers un autre. Le cout est une construction de proxy
 * par message, negligeable a l'echelle d'une conversation humaine.
 *
 * <p>Les agents sans outils ni memoire (classification, sorties structurees,
 * titrage) n'ont pas ce probleme et restent des singletons.
 */
@Configuration
@Slf4j
@Conditional(AiEnabledCondition.class)
public class AgentFactory {

    private final StreamingChatModel streamingChatModel;
    private final ChatModel chatModel;
    private final ChatModel structuredChatModel;
    private final ConversationMemoryService memoryService;
    private final AiToolFactory toolFactory;
    private final AiProperties properties;

    private final Map<Capability, String> prompts = new EnumMap<>(Capability.class);

    /**
     * Constructeur explicite : deux beans {@code ChatModel} coexistent, et Lombok ne
     * recopie pas {@code @Qualifier} sur les parametres generes. Sans cela, les deux
     * champs recevraient silencieusement le bean marque {@code @Primary}.
     */
    public AgentFactory(StreamingChatModel streamingChatModel,
                        ChatModel chatModel,
                        @Qualifier("structuredChatModel") ChatModel structuredChatModel,
                        ConversationMemoryService memoryService,
                        AiToolFactory toolFactory,
                        AiProperties properties) {
        this.streamingChatModel = streamingChatModel;
        this.chatModel = chatModel;
        this.structuredChatModel = structuredChatModel;
        this.memoryService = memoryService;
        this.toolFactory = toolFactory;
        this.properties = properties;

        prompts.put(Capability.MEDIA_PLAN, AgentPrompts.MEDIA_PLAN);
        prompts.put(Capability.REMINDER, AgentPrompts.REMINDER);
        prompts.put(Capability.PAYSLIP, AgentPrompts.PAYSLIP);
        prompts.put(Capability.GENERAL, AgentPrompts.GENERAL);
    }

    /**
     * Agent conversationnel pour une capacite et un appelant donnes.
     *
     * @param capability capacite retenue par le routage d'intention
     * @param context    identite de l'appelant, injectee dans chaque outil
     */
    public AgentDefinitions.ConversationalAgent conversationalAgent(Capability capability,
                                                                   AiCallContext context) {
        String basePrompt = prompts.getOrDefault(capability, AgentPrompts.GENERAL);
        return AiServices.builder(AgentDefinitions.ConversationalAgent.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memoryId -> memoryService.memoryFor(toConversationId(memoryId)))
                .systemMessageProvider(memoryId -> composeSystemMessage(basePrompt, memoryId))
                .tools(toolFactory.toolsFor(capability, context))
                .maxToolCallingRoundTrips(properties.getChat().getMaxToolCallingRoundTrips())
                .build();
    }

    private String composeSystemMessage(String basePrompt, Object memoryId) {
        StringBuilder sb = new StringBuilder(basePrompt);
        sb.append("\n\nDate du jour : ").append(LocalDate.now()).append('\n');
        Long conversationId = toConversationId(memoryId);
        if (conversationId != null) {
            String summary = memoryService.currentSummary(conversationId);
            if (summary != null) {
                sb.append("\n=== RESUME DES ECHANGES PRECEDENTS ===\n")
                        .append(summary)
                        .append("\n=== FIN DU RESUME ===\n");
            }
        }
        return sb.toString();
    }

    private Long toConversationId(Object memoryId) {
        if (memoryId instanceof Long id) {
            return id;
        }
        if (memoryId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    // ---- Agents sans outils ni memoire : singletons ------------------------

    @Bean
    public AgentDefinitions.IntentClassifier intentClassifier() {
        return AiServices.builder(AgentDefinitions.IntentClassifier.class)
                .chatModel(chatModel)
                .build();
    }

    @Bean
    public StructuredAgents.MediaPlanStructuredAgent mediaPlanStructuredAgent() {
        return AiServices.builder(StructuredAgents.MediaPlanStructuredAgent.class)
                .chatModel(structuredChatModel)
                .build();
    }

    @Bean
    public StructuredAgents.ReminderStructuredAgent reminderStructuredAgent() {
        return AiServices.builder(StructuredAgents.ReminderStructuredAgent.class)
                .chatModel(structuredChatModel)
                .build();
    }

    @Bean
    public StructuredAgents.PayslipStructuredAgent payslipStructuredAgent() {
        return AiServices.builder(StructuredAgents.PayslipStructuredAgent.class)
                .chatModel(structuredChatModel)
                .build();
    }

    @Bean
    public StructuredAgents.ConversationTitler conversationTitler() {
        return AiServices.builder(StructuredAgents.ConversationTitler.class)
                .chatModel(chatModel)
                .build();
    }
}
