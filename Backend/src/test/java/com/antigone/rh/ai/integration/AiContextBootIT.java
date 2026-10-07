package com.antigone.rh.ai.integration;

import com.antigone.rh.ai.agent.AgentDefinitions;
import com.antigone.rh.ai.agent.AgentFactory;
import com.antigone.rh.ai.agent.Capability;
import com.antigone.rh.ai.agent.StructuredAgents;
import com.antigone.rh.ai.support.AbstractAiIntegrationTest;
import com.antigone.rh.ai.tools.AiCallContext;
import com.antigone.rh.ai.tools.AiToolFactory;
import com.antigone.rh.security.AuthPrincipal;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifie que toute la chaine IA se construit reellement.
 *
 * <p>Les autres tests d'integration laissent {@code app.ai.chat.api-key} vide et
 * doublent les agents : les beans LLM et les proxys {@code AiServices} n'y sont donc
 * jamais instancies. Or c'est precisement a leur construction que LangChain4j lit
 * les annotations {@code @Tool} / {@code @P} et derive les schemas JSON des types de
 * retour. Un outil mal annote ou un POJO non representable en JSON Schema ne se
 * verrait qu'en production, au premier message d'un utilisateur.
 *
 * <p>Une cle factice suffit : construire un client OpenAI n'emet aucune requete.
 * Aucun appel reseau n'a lieu ici.
 */
@TestPropertySource(properties = {
        "app.ai.chat.api-key=sk-test-cle-factice-aucun-appel-reseau",
        "app.ai.embedding.dimensions=3"
})
class AiContextBootIT extends AbstractAiIntegrationTest {

    @Autowired
    private AgentFactory agentFactory;

    @Autowired
    private AiToolFactory toolFactory;

    @Autowired
    private StreamingChatModel streamingChatModel;

    @Autowired
    private ChatModel chatModel;

    @Autowired
    @Qualifier("structuredChatModel")
    private ChatModel structuredChatModel;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private AgentDefinitions.IntentClassifier intentClassifier;

    @Autowired
    private StructuredAgents.MediaPlanStructuredAgent mediaPlanAgent;

    @Autowired
    private StructuredAgents.ReminderStructuredAgent reminderAgent;

    @Autowired
    private StructuredAgents.PayslipStructuredAgent payslipAgent;

    @Autowired
    private StructuredAgents.ConversationTitler conversationTitler;

    private static AiCallContext context() {
        return AiCallContext.of(AuthPrincipal.builder()
                .principalType("EMPLOYEE")
                .accountId(1L)
                .employeId(1L)
                .username("boot-test")
                .roles(Set.of("ADMIN"))
                .permissions(Set.of("VIEW_MEDIA_PLAN", "VIEW_FINANCE"))
                .build(), 1L);
    }

    @Test
    @DisplayName("Tous les modeles sont instancies quand une cle est fournie")
    void allModelsAreInstantiated() {
        assertThat(streamingChatModel).isNotNull();
        assertThat(chatModel).isNotNull();
        assertThat(structuredChatModel).isNotNull();
        assertThat(embeddingModel).isNotNull();
    }

    @Test
    @DisplayName("Les deux ChatModel sont bien des beans distincts")
    void primaryAndStructuredChatModelsAreDistinct() {
        // @Qualifier n'etant pas recopie par Lombok sur un constructeur genere, une
        // regression ici ferait silencieusement pointer les deux champs sur le meme
        // bean, et les sorties structurees perdraient strictJsonSchema.
        assertThat(structuredChatModel).isNotSameAs(chatModel);
    }

    @ParameterizedTest
    @EnumSource(Capability.class)
    @DisplayName("Un agent conversationnel se construit pour chaque capacite")
    void conversationalAgentBuildsForEveryCapability(Capability capability) {
        // La construction du proxy declenche la lecture des annotations @Tool et la
        // generation des specifications d'outils : c'est la que casserait un outil
        // mal annote.
        assertThatCode(() -> {
            AgentDefinitions.ConversationalAgent agent =
                    agentFactory.conversationalAgent(capability, context());
            assertThat(agent).isNotNull();
        }).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(Capability.class)
    @DisplayName("Chaque capacite expose un jeu d'outils non vide")
    void everyCapabilityExposesTools(Capability capability) {
        List<Object> tools = toolFactory.toolsFor(capability, context());

        assertThat(tools).isNotEmpty();
        assertThat(tools).doesNotContainNull();
    }

    @Test
    @DisplayName("Les agents a sortie structuree sont instancies")
    void structuredAgentsAreInstantiated() {
        assertThat(mediaPlanAgent).isNotNull();
        assertThat(reminderAgent).isNotNull();
        assertThat(payslipAgent).isNotNull();
        assertThat(intentClassifier).isNotNull();
        assertThat(conversationTitler).isNotNull();
    }

    @Test
    @DisplayName("Les outils sont bien lies a leur appelant, pas partages")
    void toolsAreBoundToTheirCaller() {
        List<Object> first = toolFactory.toolsFor(Capability.MEDIA_PLAN, context());
        List<Object> second = toolFactory.toolsFor(Capability.MEDIA_PLAN, context());

        // Deux appels donnent deux jeux d'instances : c'est ce qui garantit qu'un
        // outil ne peut pas servir un autre utilisateur que celui qui l'a cree.
        assertThat(first.get(0)).isNotSameAs(second.get(0));
    }
}
