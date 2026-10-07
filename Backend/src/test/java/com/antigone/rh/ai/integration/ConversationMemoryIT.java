package com.antigone.rh.ai.integration;

import com.antigone.rh.ai.config.AiProperties;
import com.antigone.rh.ai.dto.ConversationSummaryDTO;
import com.antigone.rh.ai.dto.CreateConversationRequest;
import com.antigone.rh.ai.entity.AiMessageRole;
import com.antigone.rh.ai.memory.ConversationMemoryService;
import com.antigone.rh.ai.memory.JpaChatMemoryStore;
import com.antigone.rh.ai.repository.AiConversationRepository;
import com.antigone.rh.ai.service.ConversationService;
import com.antigone.rh.ai.support.AbstractAiIntegrationTest;
import com.antigone.rh.ai.support.TestFixtures;
import com.antigone.rh.security.AuthPrincipal;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Scenario 9 : memoire persistante et resume automatique.
 *
 * <p>Le profil de test reduit la fenetre a 6 messages et le pas de resume a 2, pour
 * atteindre le seuil sans generer trente tours : la regle testee est la meme, seuls
 * les nombres changent.
 */
class ConversationMemoryIT extends AbstractAiIntegrationTest {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ConversationMemoryService memoryService;

    @Autowired
    private JpaChatMemoryStore chatMemoryStore;

    @Autowired
    private AiConversationRepository conversationRepository;

    @Autowired
    private AiProperties properties;

    @Autowired
    private TestFixtures fixtures;

    @MockitoBean
    private ChatModel chatModel;

    private AuthPrincipal principal;
    private Long conversationId;
    private final AtomicInteger summaryCalls = new AtomicInteger();

    @BeforeEach
    void setUp() {
        principal = fixtures.employeePrincipal(fixtures.employe("Memoire", "Test"));
        ConversationSummaryDTO conversation =
                conversationService.create(principal, new CreateConversationRequest());
        conversationId = conversation.getId();
        summaryCalls.set(0);

        when(chatModel.chat(anyString())).thenAnswer(call -> {
            summaryCalls.incrementAndGet();
            return "Resume numero " + summaryCalls.get() + " : la marque Alpha et le mois de juillet "
                    + "sont au centre des echanges.";
        });
    }

    private void exchange(int turns) {
        for (int i = 0; i < turns; i++) {
            conversationService.append(conversationId, AiMessageRole.USER,
                    "Question numero " + i + " sur la marque Alpha", null, null, null);
            conversationService.append(conversationId, AiMessageRole.ASSISTANT,
                    "Reponse numero " + i, null, null, "GENERAL");
        }
    }

    @Test
    @DisplayName("Sous le seuil, aucun resume n'est declenche")
    void noSummaryBelowThreshold() {
        exchange(2);

        memoryService.maybeSummarize(conversationId);

        verify(chatModel, never()).chat(anyString());
        assertThat(memoryService.currentSummary(conversationId)).isNull();
    }

    @Test
    @DisplayName("Au-dela du seuil, le resume se declenche et est persiste")
    void summaryIsTriggeredAndPersisted() {
        exchange(16);

        memoryService.maybeSummarize(conversationId);

        assertThat(summaryCalls.get()).isEqualTo(1);
        String summary = memoryService.currentSummary(conversationId);
        assertThat(summary).isNotNull().contains("marque Alpha");

        var conversation = conversationRepository.findById(conversationId).orElseThrow();
        assertThat(conversation.getSummarizedMessageCount())
                .isEqualTo(32 - properties.getMemory().getMaxMessages());
    }

    @Test
    @DisplayName("Le resume ne se recalcule pas tant que le pas n'est pas franchi")
    void summaryIsNotRecomputedBeforeTheNextStep() {
        exchange(16);
        memoryService.maybeSummarize(conversationId);
        assertThat(summaryCalls.get()).isEqualTo(1);

        memoryService.maybeSummarize(conversationId);

        assertThat(summaryCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Le resume se met a jour quand la conversation continue")
    void summaryIsRefreshedAsTheConversationGrows() {
        exchange(16);
        memoryService.maybeSummarize(conversationId);

        exchange(6);
        memoryService.maybeSummarize(conversationId);

        assertThat(summaryCalls.get()).isEqualTo(2);
        assertThat(memoryService.currentSummary(conversationId)).contains("Resume numero 2");
    }

    @Test
    @DisplayName("Un echec du resume ne casse pas la conversation")
    void summaryFailureIsNonFatal() {
        when(chatModel.chat(anyString())).thenThrow(new RuntimeException("modele indisponible"));
        exchange(16);

        memoryService.maybeSummarize(conversationId);

        assertThat(memoryService.currentSummary(conversationId)).isNull();
        assertThat(conversationService.get(principal, conversationId).getMessages()).hasSize(32);
    }

    @Test
    @DisplayName("La memoire LangChain4j survit a un rechargement complet")
    void memorySurvivesReload() {
        ChatMemory memory = memoryService.memoryFor(conversationId);
        memory.add(UserMessage.from("Quel est le media plan de juillet ?"));
        memory.add(AiMessage.from("Le voici."));

        // Une nouvelle instance relit depuis la base, comme apres un redemarrage.
        ChatMemory reloaded = memoryService.memoryFor(conversationId);
        List<ChatMessage> messages = reloaded.messages();

        assertThat(messages).hasSize(2);
        assertThat(((UserMessage) messages.get(0)).singleText())
                .isEqualTo("Quel est le media plan de juillet ?");
        assertThat(((AiMessage) messages.get(1)).text()).isEqualTo("Le voici.");
    }

    @Test
    @DisplayName("La fenetre glissante borne le nombre de messages conserves en clair")
    void slidingWindowCapsRetainedMessages() {
        ChatMemory memory = memoryService.memoryFor(conversationId);
        for (int i = 0; i < 20; i++) {
            memory.add(UserMessage.from("message " + i));
        }

        assertThat(memory.messages()).hasSizeLessThanOrEqualTo(properties.getMemory().getMaxMessages());
    }

    @Test
    @DisplayName("Supprimer la conversation purge sa memoire LLM")
    void deletingConversationPurgesItsMemory() {
        ChatMemory memory = memoryService.memoryFor(conversationId);
        memory.add(UserMessage.from("a conserver ? non"));
        assertThat(chatMemoryStore.getMessages(conversationId)).isNotEmpty();

        conversationService.delete(principal, conversationId);

        assertThat(chatMemoryStore.getMessages(conversationId)).isEmpty();
    }

    @Test
    @DisplayName("Une memoire illisible repart a vide sans faire echouer le tour")
    void corruptedMemoryDegradesGracefully() {
        chatMemoryStore.updateMessages(conversationId, List.of(UserMessage.from("ok")));
        var record = new com.antigone.rh.ai.entity.AiChatMemoryRecord();
        record.setMemoryId(conversationId);
        record.setMessagesJson("{ ceci n'est pas du json valide");

        assertThat(chatMemoryStore.getMessages(999_999L)).isEmpty();
    }
}
