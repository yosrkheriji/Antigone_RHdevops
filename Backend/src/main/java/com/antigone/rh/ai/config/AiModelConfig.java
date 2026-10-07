package com.antigone.rh.ai.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Beans LLM, créés uniquement quand {@link AiEnabledCondition} est satisfaite.
 * Sans clé API l'application démarre normalement : seuls les endpoints IA
 * répondent 503 SERVICE_UNAVAILABLE, le reste du backend RH est intact.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@Conditional(AiEnabledCondition.class)
public class AiModelConfig {

    private final AiProperties properties;

    /**
     * Modèle streaming — utilisé par tous les endpoints exposés au frontend.
     * Le streaming est imposé : jamais de réponse bloquante attendant la fin
     * complète de la génération.
     */
    @Bean
    public StreamingChatModel streamingChatModel() {
        AiProperties.Chat chat = properties.getChat();
        log.info("Chat streaming : modèle={} baseUrl={} timeout={}",
                chat.getModel(), chat.getBaseUrl(), chat.getTimeout());
        return OpenAiStreamingChatModel.builder()
                .apiKey(chat.getApiKey())
                .baseUrl(chat.getBaseUrl())
                .modelName(chat.getModel())
                .temperature(chat.getTemperature())
                .timeout(AiProperties.effective(chat.getTimeout()))
                .logRequests(chat.isLogRequests())
                .logResponses(chat.isLogResponses())
                .build();
    }

    /**
     * Modèle synchrone — étapes internes jamais exposées en streaming : résumé de
     * mémoire, génération JSON structurée du media plan, titrage automatique.
     */
    @Bean
    @org.springframework.context.annotation.Primary
    public ChatModel chatModel() {
        AiProperties.Chat chat = properties.getChat();
        return OpenAiChatModel.builder()
                .apiKey(chat.getApiKey())
                .baseUrl(chat.getBaseUrl())
                .modelName(chat.getModel())
                .temperature(chat.getTemperature())
                .timeout(AiProperties.effective(chat.getTimeout()))
                .maxRetries(chat.getMaxRetries())
                .logRequests(chat.isLogRequests())
                .logResponses(chat.isLogResponses())
                .build();
    }

    /**
     * Modele dedie aux sorties structurees. {@code strictJsonSchema} active le mode
     * JSON Schema natif d'OpenAI : le modele ne peut alors physiquement pas produire
     * un objet hors schema, ce qui supprime toute etape de reparsing defensif.
     * Temperature basse : on veut une structure stable, pas de la variete.
     */
    @Bean("structuredChatModel")
    public ChatModel structuredChatModel() {
        AiProperties.Chat chat = properties.getChat();
        return OpenAiChatModel.builder()
                .apiKey(chat.getApiKey())
                .baseUrl(chat.getBaseUrl())
                .modelName(chat.getModel())
                .temperature(0.4)
                .timeout(AiProperties.effective(chat.getTimeout()))
                .maxRetries(chat.getMaxRetries())
                .strictJsonSchema(true)
                .logRequests(chat.isLogRequests())
                .logResponses(chat.isLogResponses())
                .build();
    }

    /** Branche dense du RAG hybride. Retombe sur la clé de chat si non spécifiée. */
    @Bean
    public EmbeddingModel embeddingModel() {
        AiProperties.Embedding embedding = properties.getEmbedding();
        String apiKey = (embedding.getApiKey() == null || embedding.getApiKey().isBlank())
                ? properties.getChat().getApiKey()
                : embedding.getApiKey();
        log.info("Embeddings : modèle={} dimensions={}", embedding.getModel(), embedding.getDimensions());
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(embedding.getBaseUrl())
                .modelName(embedding.getModel())
                .dimensions(embedding.getDimensions())
                .timeout(AiProperties.effective(embedding.getTimeout()))
                .maxRetries(embedding.getMaxRetries())
                .build();
    }
}
