package com.antigone.rh.ai.service;

import com.antigone.rh.ai.agent.StructuredAgents;
import com.antigone.rh.ai.dto.ConversationDetailDTO;
import com.antigone.rh.ai.dto.ConversationSummaryDTO;
import com.antigone.rh.ai.dto.CreateConversationRequest;
import com.antigone.rh.ai.dto.MessageDTO;
import com.antigone.rh.ai.dto.UpdateConversationRequest;
import com.antigone.rh.ai.entity.AiConversation;
import com.antigone.rh.ai.entity.AiMessage;
import com.antigone.rh.ai.entity.AiMessageRole;
import com.antigone.rh.ai.memory.JpaChatMemoryStore;
import com.antigone.rh.ai.repository.AiConversationRepository;
import com.antigone.rh.ai.repository.AiMessageRepository;
import com.antigone.rh.exception.ResourceNotFoundException;
import com.antigone.rh.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CRUD des conversations et journal des messages.
 *
 * <p>Cloisonnement strict : chaque lecture passe par
 * {@code findOwnedById(id, compteId)}. Un identifiant de conversation appartenant a
 * un autre compte produit un 404, pas un 403 — repondre « interdit » confirmerait
 * l'existence de la ressource.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private static final String DEFAULT_TITLE = "Nouvelle conversation";

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final JpaChatMemoryStore chatMemoryStore;
    private final ObjectProvider<StructuredAgents.ConversationTitler> titlerProvider;

    // ---- Lecture -----------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ConversationSummaryDTO> list(AuthPrincipal principal, Pageable pageable) {
        return conversationRepository.findOwnedBy(principal.getAccountId(), pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public ConversationDetailDTO get(AuthPrincipal principal, Long conversationId) {
        AiConversation conversation = requireOwned(principal, conversationId);
        List<MessageDTO> messages = messageRepository
                .findByConversationIdOrderBySequenceAsc(conversationId).stream()
                .map(this::toMessageDto)
                .toList();
        return ConversationDetailDTO.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .pinned(conversation.getPinned())
                .summary(conversation.getSummary())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .messages(messages)
                .build();
    }

    /** Charge la conversation en verifiant la propriete. Leve 404 sinon. */
    @Transactional(readOnly = true)
    public AiConversation requireOwned(AuthPrincipal principal, Long conversationId) {
        return conversationRepository.findOwnedById(conversationId, principal.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));
    }

    // ---- Ecriture ----------------------------------------------------------

    @Transactional
    public ConversationSummaryDTO create(AuthPrincipal principal, CreateConversationRequest request) {
        String title = request != null && request.getTitle() != null && !request.getTitle().isBlank()
                ? request.getTitle().trim()
                : DEFAULT_TITLE;
        AiConversation conversation = conversationRepository.save(AiConversation.builder()
                .compteId(principal.getAccountId())
                .title(title)
                .build());
        log.debug("Conversation {} creee pour le compte {}", conversation.getId(), principal.getAccountId());
        return toSummary(conversation);
    }

    @Transactional
    public ConversationSummaryDTO update(AuthPrincipal principal, Long conversationId,
                                         UpdateConversationRequest request) {
        AiConversation conversation = requireOwned(principal, conversationId);
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            conversation.setTitle(request.getTitle().trim());
        }
        if (request.getPinned() != null) {
            conversation.setPinned(request.getPinned());
        }
        return toSummary(conversationRepository.save(conversation));
    }

    /**
     * Soft-delete : la conversation sort de l'API mais reste disponible pour l'audit.
     * La memoire LangChain4j, elle, est reellement purgee — la conserver n'aurait
     * aucun usage et occuperait du contexte.
     */
    @Transactional
    public void delete(AuthPrincipal principal, Long conversationId) {
        AiConversation conversation = requireOwned(principal, conversationId);
        conversation.setDeletedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        chatMemoryStore.deleteMessages(conversationId);
        log.debug("Conversation {} supprimee (soft) par le compte {}", conversationId, principal.getAccountId());
    }

    // ---- Journal des messages ----------------------------------------------

    /** Ajoute un message et fait avancer le compteur d'ordre de la conversation. */
    @Transactional
    public AiMessage append(Long conversationId, AiMessageRole role, String content,
                            String toolCalls, String structuredResult, String capability) {
        AiConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));
        long sequence = conversation.getNextSequence() == null ? 0L : conversation.getNextSequence();
        conversation.setNextSequence(sequence + 1);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return messageRepository.save(AiMessage.builder()
                .conversationId(conversationId)
                .role(role)
                .content(content)
                .toolCalls(toolCalls)
                .structuredResult(structuredResult)
                .capability(capability)
                .sequence(sequence)
                .build());
    }

    /**
     * Donne un titre a la conversation depuis son premier message. Silencieux en cas
     * d'echec : un titre par defaut est un desagrement, pas une panne.
     */
    @Transactional
    public void ensureTitle(Long conversationId, String firstMessage) {
        AiConversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null || !DEFAULT_TITLE.equals(conversation.getTitle())) {
            return;
        }
        String title = null;
        StructuredAgents.ConversationTitler titler = titlerProvider.getIfAvailable();
        if (titler != null) {
            try {
                title = titler.title(firstMessage);
            } catch (Exception e) {
                log.debug("Titrage automatique en echec : {}", e.getMessage());
            }
        }
        if (title == null || title.isBlank()) {
            title = firstMessage.length() <= 60 ? firstMessage : firstMessage.substring(0, 57) + "...";
        }
        conversation.setTitle(title.trim().replace("\"", "").substring(0, Math.min(200, title.trim().length())));
        conversationRepository.save(conversation);
    }

    // ---- Mapping -----------------------------------------------------------

    private ConversationSummaryDTO toSummary(AiConversation conversation) {
        return ConversationSummaryDTO.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .pinned(conversation.getPinned())
                .messageCount(messageRepository.countByConversationId(conversation.getId()))
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    private MessageDTO toMessageDto(AiMessage message) {
        return MessageDTO.builder()
                .id(message.getId())
                .role(message.getRole().name())
                .content(message.getContent())
                .toolCalls(message.getToolCalls())
                .structuredResult(message.getStructuredResult())
                .capability(message.getCapability())
                .sequence(message.getSequence())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
