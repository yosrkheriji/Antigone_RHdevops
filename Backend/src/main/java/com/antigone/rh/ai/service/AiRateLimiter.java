package com.antigone.rh.ai.service;

import com.antigone.rh.ai.config.AiProperties;
import com.antigone.rh.ai.exception.AiRateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitation de debit des appels IA, par compte.
 *
 * <p>Chaque appel au LLM a un cout reel : sans garde-fou, une boucle cote client ou
 * un onglet laisse ouvert peut consommer un budget entier. Une fenetre glissante en
 * memoire suffit ici — l'application tourne en instance unique, et une dependance
 * externe type Redis serait disproportionnee.
 *
 * <p>Corollaire assume : en cas de montee en charge multi-instances, le quota
 * deviendrait par instance. C'est le moment ou il faudra basculer sur un compteur
 * partage.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiRateLimiter {

    private final AiProperties properties;
    private final Map<Long, Deque<Long>> callsByAccount = new ConcurrentHashMap<>();

    /**
     * Enregistre un appel et refuse au-dela du quota.
     *
     * @throws AiRateLimitException si la fenetre courante est saturee
     */
    public void checkAndRecord(Long accountId) {
        AiProperties.RateLimit config = properties.getRateLimit();
        if (!config.isEnabled() || accountId == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long windowStart = now - config.getWindow().toMillis();

        Deque<Long> calls = callsByAccount.computeIfAbsent(accountId, key -> new ArrayDeque<>());
        synchronized (calls) {
            while (!calls.isEmpty() && calls.peekFirst() < windowStart) {
                calls.pollFirst();
            }
            if (calls.size() >= config.getRequestsPerWindow()) {
                long retryInSeconds = Math.max(1,
                        (calls.peekFirst() + config.getWindow().toMillis() - now) / 1000);
                log.warn("Quota IA atteint pour le compte {} ({} appels / {})",
                        accountId, config.getRequestsPerWindow(), config.getWindow());
                throw new AiRateLimitException(
                        "Quota d'appels a l'assistant atteint. Reessayez dans " + retryInSeconds + " secondes.");
            }
            calls.addLast(now);
        }
    }

    /** Purge les comptes inactifs pour que la table ne croisse pas indefiniment. */
    @Scheduled(fixedDelay = 600_000L)
    public void evictIdleAccounts() {
        long threshold = System.currentTimeMillis() - Duration.ofMinutes(30).toMillis();
        callsByAccount.entrySet().removeIf(entry -> {
            Deque<Long> calls = entry.getValue();
            synchronized (calls) {
                return calls.isEmpty() || calls.peekLast() < threshold;
            }
        });
    }
}
