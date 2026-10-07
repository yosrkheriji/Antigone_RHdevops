package com.antigone.rh.ai.support;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * Saute les tests d'integration quand aucun demon Docker n'est joignable.
 *
 * <p>Implemente comme {@link ExecutionCondition} plutot que via {@code @EnabledIf} :
 * les annotations de condition de JUnit ne sont pas heritees, alors que
 * {@code @ExtendWith} l'est. Pose sur la classe de base, la regle couvre donc
 * automatiquement toutes les classes de test qui en heritent.
 *
 * <p>Une condition s'evalue avant tout {@code BeforeAllCallback}, donc avant que
 * Testcontainers ne tente de demarrer le conteneur.
 */
public class DockerAvailableCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED =
            ConditionEvaluationResult.enabled("Docker disponible");

    private static final ConditionEvaluationResult DISABLED = ConditionEvaluationResult.disabled(
            "Docker indisponible : test d'integration saute. Demarrez Docker puis relancez `mvn verify`.");

    /** Sonde une seule fois : l'echec de detection prend plusieurs secondes. */
    private static volatile Boolean cachedAvailability;

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return isDockerAvailable() ? ENABLED : DISABLED;
    }

    private static boolean isDockerAvailable() {
        Boolean cached = cachedAvailability;
        if (cached != null) {
            return cached;
        }
        synchronized (DockerAvailableCondition.class) {
            if (cachedAvailability == null) {
                boolean available;
                try {
                    available = DockerClientFactory.instance().isDockerAvailable();
                } catch (Throwable ignored) {
                    available = false;
                }
                cachedAvailability = available;
            }
            return cachedAvailability;
        }
    }
}
