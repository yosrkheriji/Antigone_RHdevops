package com.antigone.rh.ai.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Les beans LLM ne sont créés que si l'assistant est activé <em>et</em> qu'une clé
 * API non vide est fournie. {@code @ConditionalOnProperty} ne suffit pas : une clé
 * définie mais vide (cas classique d'une variable d'environnement non renseignée
 * en CI) satisferait la condition et ferait échouer la construction du client.
 */
public class AiEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        var env = context.getEnvironment();
        if (!env.getProperty("app.ai.enabled", Boolean.class, Boolean.TRUE)) {
            return false;
        }
        String apiKey = env.getProperty("app.ai.chat.api-key", "");
        return apiKey != null && !apiKey.isBlank();
    }
}
