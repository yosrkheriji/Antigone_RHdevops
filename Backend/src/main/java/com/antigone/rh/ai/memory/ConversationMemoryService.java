package com.antigone.rh.ai.memory;

import com.antigone.rh.ai.config.AiProperties;
import com.antigone.rh.ai.entity.AiConversation;
import com.antigone.rh.ai.entity.AiMessage;
import com.antigone.rh.ai.repository.AiConversationRepository;
import com.antigone.rh.ai.repository.AiMessageRepository;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Fenetre de contexte et resume automatique.
 *
 * <p>Les {@code app.ai.memory.max-messages} derniers messages restent en clair.
 * Au-dela, les plus anciens sont replies dans un resume cumulatif reinjecte en
 * tete du prompt systeme, ce qui borne la taille du contexte tout en gardant la
 * conversation coherente sur plusieurs dizaines de tours.
 *
 * <p>Le resume est calcule depuis {@code ai_messages} — le journal durable — et non
 * depuis la fenetre LangChain4j : la logique reste ainsi independante de la
 * strategie d'eviction de la librairie, et directement testable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationMemoryService {

    private static final String SUMMARY_PROMPT = """
            Tu resumes un historique de conversation entre un collaborateur et l'assistant \
            interne d'Antigone, une agence de communication.

            Produis un resume factuel et dense, en francais, de 150 mots maximum. Conserve \
            imperativement : les marques, projets, mois et montants cites ; les decisions \
            prises ; les preferences exprimees par l'utilisateur ; les questions restees \
            sans reponse. N'invente rien et n'ajoute aucun commentaire.

            Si un resume anterieur est fourni, integre-le : ta reponse doit le remplacer \
            entierement, pas le completer.
            """;

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final JpaChatMemoryStore chatMemoryStore;
    private final AiProperties properties;
    private final ObjectProvider<ChatModel> chatModelProvider;

    /** Fenetre glissante persistee, une par conversation. */
    public ChatMemory memoryFor(Long conversationId) {
        return MessageWindowChatMemory.builder()
                .id(conversationId)
                .maxMessages(properties.getMemory().getMaxMessages())
                .chatMemoryStore(chatMemoryStore)
                .build();
    }

    /**
     * Replie dans le resume les messages sortis de la fenetre.
     *
     * <p>Declenche lorsque le nombre de messages non encore resumes depasse la
     * fenetre d'au moins {@code summarizeEvery}, afin de ne pas relancer un appel
     * LLM a chaque tour.
     */
    @Transactional
    public void maybeSummarize(Long conversationId) {
        AiProperties.Memory config = properties.getMemory();
        if (!config.isSummarizationEnabled()) {
            return;
        }
        ChatModel model = chatModelProvider.getIfAvailable();
        if (model == null) {
            return;
        }
        AiConversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null) {
            return;
        }

        List<AiMessage> all = messageRepository.findByConversationIdOrderBySequenceAsc(conversationId);
        int alreadySummarized = conversation.getSummarizedMessageCount() == null
                ? 0
                : conversation.getSummarizedMessageCount();
        int keptInWindow = config.getMaxMessages();
        int summarizableUpTo = all.size() - keptInWindow;

        if (summarizableUpTo - alreadySummarized < config.getSummarizeEvery()) {
            return;
        }

        List<AiMessage> toSummarize = all.subList(alreadySummarized, summarizableUpTo);
        if (toSummarize.isEmpty()) {
            return;
        }

        String prompt = buildPrompt(conversation.getSummary(), toSummarize);
        try {
            String summary = model.chat(SUMMARY_PROMPT + "\n\n" + prompt);
            conversation.setSummary(summary == null ? conversation.getSummary() : summary.trim());
            conversation.setSummarizedMessageCount(summarizableUpTo);
            conversationRepository.save(conversation);
            log.debug("Conversation {} : {} messages replies dans le resume (total resume : {})",
                    conversationId, toSummarize.size(), summarizableUpTo);
        } catch (Exception e) {
            // Un resume manquant degrade le contexte long mais ne doit jamais faire
            // echouer la conversation : on retentera au tour suivant.
            log.warn("Resume de la conversation {} en echec : {}", conversationId, e.getMessage());
        }
    }

    /** Resume courant, reinjecte dans le prompt systeme. Null si aucun. */
    @Transactional(readOnly = true)
    public String currentSummary(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .map(AiConversation::getSummary)
                .filter(summary -> summary != null && !summary.isBlank())
                .orElse(null);
    }

    private String buildPrompt(String previousSummary, List<AiMessage> messages) {
        StringBuilder sb = new StringBuilder();
        if (previousSummary != null && !previousSummary.isBlank()) {
            sb.append("=== RESUME ANTERIEUR ===\n").append(previousSummary).append("\n\n");
        }
        sb.append("=== ECHANGES A INTEGRER ===\n");
        for (AiMessage message : messages) {
            String content = message.getContent();
            if (content == null || content.isBlank()) {
                continue;
            }
            sb.append(message.getRole().name()).append(" : ").append(truncate(content, 2000)).append('\n');
        }
        return sb.toString();
    }

    private String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
