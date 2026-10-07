package com.antigone.rh.ai.memory;

import com.antigone.rh.ai.entity.AiChatMemoryRecord;
import com.antigone.rh.ai.repository.AiChatMemoryRepository;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Memoire conversationnelle LangChain4j adossee a PostgreSQL.
 *
 * <p>Une ligne par conversation, contenant la fenetre courante serialisee. La
 * conversation survit donc a un redemarrage du service, contrairement au
 * {@code InMemoryChatMemoryStore} par defaut.
 *
 * <p>Cette classe ne fait que persister : le resume des echanges anciens est gere
 * par {@link ConversationMemoryService}, qui travaille sur le journal durable
 * {@code ai_messages} plutot que d'essayer de rattraper les messages evinces par
 * la fenetre glissante.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaChatMemoryStore implements ChatMemoryStore {

    private final AiChatMemoryRepository memoryRepository;

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<ChatMessage> getMessages(Object memoryId) {
        Long id = toId(memoryId);
        if (id == null) {
            return List.of();
        }
        return memoryRepository.findById(id)
                .map(AiChatMemoryRecord::getMessagesJson)
                .filter(json -> json != null && !json.isBlank())
                .map(json -> {
                    try {
                        return ChatMessageDeserializer.messagesFromJson(json);
                    } catch (Exception e) {
                        // Format illisible (montee de version, donnee corrompue) : mieux
                        // vaut repartir d'une fenetre vide que de faire echouer le tour.
                        log.warn("Memoire illisible pour la conversation {} ({}) - repartie a vide",
                                id, e.getMessage());
                        return List.<ChatMessage>of();
                    }
                })
                .orElseGet(List::of);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        Long id = toId(memoryId);
        if (id == null) {
            return;
        }
        AiChatMemoryRecord record = memoryRepository.findById(id)
                .orElseGet(() -> AiChatMemoryRecord.builder().memoryId(id).build());
        record.setMessagesJson(ChatMessageSerializer.messagesToJson(messages));
        memoryRepository.save(record);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteMessages(Object memoryId) {
        Long id = toId(memoryId);
        if (id != null) {
            memoryRepository.deleteById(id);
        }
    }

    private Long toId(Object memoryId) {
        if (memoryId instanceof Long id) {
            return id;
        }
        if (memoryId instanceof Number number) {
            return number.longValue();
        }
        if (memoryId instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
