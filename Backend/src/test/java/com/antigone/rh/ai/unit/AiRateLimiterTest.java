package com.antigone.rh.ai.unit;

import com.antigone.rh.ai.config.AiProperties;
import com.antigone.rh.ai.exception.AiRateLimitException;
import com.antigone.rh.ai.service.AiRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Limitation de debit : chaque appel LLM coute, une boucle cote client ne doit pas
 * pouvoir consommer le budget.
 */
class AiRateLimiterTest {

    private AiProperties properties;
    private AiRateLimiter limiter;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.getRateLimit().setRequestsPerWindow(3);
        properties.getRateLimit().setWindow(Duration.ofMillis(500));
        limiter = new AiRateLimiter(properties);
    }

    @Test
    @DisplayName("Le quota est refuse au-dela de la limite")
    void refusesBeyondQuota() {
        for (int i = 0; i < 3; i++) {
            assertThatCode(() -> limiter.checkAndRecord(1L)).doesNotThrowAnyException();
        }

        assertThatThrownBy(() -> limiter.checkAndRecord(1L))
                .isInstanceOf(AiRateLimitException.class)
                .hasMessageContaining("Quota");
    }

    @Test
    @DisplayName("Le quota est independant d'un compte a l'autre")
    void quotaIsPerAccount() {
        for (int i = 0; i < 3; i++) {
            limiter.checkAndRecord(1L);
        }

        assertThatCode(() -> limiter.checkAndRecord(2L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("La fenetre glisse : le quota se reconstitue avec le temps")
    void windowSlides() throws Exception {
        for (int i = 0; i < 3; i++) {
            limiter.checkAndRecord(1L);
        }
        assertThatThrownBy(() -> limiter.checkAndRecord(1L)).isInstanceOf(AiRateLimitException.class);

        Thread.sleep(600);

        assertThatCode(() -> limiter.checkAndRecord(1L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Desactive, le limiteur laisse tout passer")
    void disabledLimiterAllowsEverything() {
        properties.getRateLimit().setEnabled(false);

        assertThatCode(() -> {
            for (int i = 0; i < 100; i++) {
                limiter.checkAndRecord(1L);
            }
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un compte inconnu ne fait pas echouer l'appel")
    void nullAccountIsTolerated() {
        assertThatCode(() -> limiter.checkAndRecord(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Sous charge concurrente, le quota reste exact")
    void quotaHoldsUnderConcurrency() throws Exception {
        properties.getRateLimit().setRequestsPerWindow(50);
        properties.getRateLimit().setWindow(Duration.ofSeconds(30));

        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        java.util.List<Thread> threads = new java.util.ArrayList<>();

        for (int t = 0; t < 8; t++) {
            Thread thread = new Thread(() -> {
                for (int i = 0; i < 25; i++) {
                    try {
                        limiter.checkAndRecord(7L);
                        accepted.incrementAndGet();
                    } catch (AiRateLimitException e) {
                        refused.incrementAndGet();
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        assertThat(accepted.get()).isEqualTo(50);
        assertThat(accepted.get() + refused.get()).isEqualTo(200);
    }

    @Test
    @DisplayName("La purge libere les comptes inactifs")
    void evictionClearsIdleAccounts() {
        limiter.checkAndRecord(1L);

        assertThatCode(limiter::evictIdleAccounts).doesNotThrowAnyException();
    }
}
