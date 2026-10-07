package com.antigone.rh.ai.sse;

import com.antigone.rh.ai.config.AiAsyncConfig;
import com.antigone.rh.ai.exception.AiForbiddenException;
import com.antigone.rh.ai.exception.AiRateLimitException;
import com.antigone.rh.ai.exception.AiUnavailableException;
import com.antigone.rh.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.function.Function;

/**
 * Execute une generation longue sur le pool IA et en rend le resultat en SSE.
 *
 * <p>Sert les generations deterministes (media plan, relance, paie), la ou
 * {@code ChatOrchestratorService} sert le chat libre. Le resultat final part en un
 * seul evenement {@code structured_result}, precede des evenements de progression
 * emis par la tache elle-meme.
 *
 * <p>Le {@code SecurityContext} de la requete est recopie explicitement sur le
 * thread d'execution : les services appeles s'appuient sur
 * {@code SecurityContextHolder}, qui n'est pas propage a un pool.
 */
@Component
@Slf4j
public class AiTaskStreamer {

    private final ThreadPoolTaskExecutor executor;

    public AiTaskStreamer(@Qualifier(AiAsyncConfig.AI_EXECUTOR) ThreadPoolTaskExecutor executor) {
        this.executor = executor;
    }

    /**
     * @param task recoit la session pour emettre sa progression, et rend le resultat
     *             final publie dans {@code structured_result}
     */
    public <T> SseEmitter run(AiSseSession session, Function<AiSseSession, T> task) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        try {
            executor.execute(() -> {
                SecurityContextHolder.setContext(securityContext);
                try {
                    T result = task.apply(session);
                    session.structuredResult(result);
                } catch (AiForbiddenException e) {
                    session.error("FORBIDDEN", e.getMessage());
                } catch (AiUnavailableException e) {
                    session.error("AI_UNAVAILABLE", e.getMessage());
                } catch (AiRateLimitException e) {
                    session.error("RATE_LIMITED", e.getMessage());
                } catch (ResourceNotFoundException e) {
                    session.error("NOT_FOUND", e.getMessage());
                } catch (IllegalArgumentException e) {
                    session.error("BAD_REQUEST", e.getMessage());
                } catch (Exception e) {
                    log.error("Generation en echec : {}", e.getMessage(), e);
                    session.error("GENERATION_FAILED", safeMessage(e));
                } finally {
                    SecurityContextHolder.clearContext();
                    session.complete();
                }
            });
        } catch (Exception e) {
            log.warn("Generation refusee (pool sature) : {}", e.getMessage());
            session.error("AI_BUSY", "L'assistant traite trop de demandes. Reessayez dans un instant.");
            session.complete();
        }
        return session.emitter();
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return message.length() <= 300 ? message : message.substring(0, 300) + "...";
    }
}
