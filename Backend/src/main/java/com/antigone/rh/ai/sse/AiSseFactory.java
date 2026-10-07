package com.antigone.rh.ai.sse;

import com.antigone.rh.ai.config.AiAsyncConfig;
import com.antigone.rh.ai.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

/**
 * Fabrique les flux SSE et arme leur heartbeat.
 *
 * <p>Le heartbeat est indispensable : un appel d'outil Drive peut occuper 40 s sans
 * qu'aucun token ne soit genere, et la plupart des proxys (Nginx, ALB) ferment une
 * connexion inactive bien avant. Un evenement periodique la maintient ouverte.
 */
@Component
@Slf4j
public class AiSseFactory {

    private final ObjectMapper objectMapper;
    private final AiProperties properties;
    private final ThreadPoolTaskScheduler scheduler;

    public AiSseFactory(ObjectMapper objectMapper,
                        AiProperties properties,
                        @Qualifier(AiAsyncConfig.AI_HEARTBEAT_SCHEDULER) ThreadPoolTaskScheduler scheduler) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.scheduler = scheduler;
    }

    public AiSseSession open() {
        AiProperties.Sse config = properties.getSse();
        // `null` signifie explicitement « pas d'expiration » cote Spring MVC : le
        // flux vit aussi longtemps que la generation, sans minuteur qui le coupe.
        Long timeoutMillis = AiProperties.isUnlimited(config.getTimeout())
                ? null
                : config.getTimeout().toMillis();
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        AiSseSession session = new AiSseSession(emitter, objectMapper, config.getHeartbeatInterval());

        Duration interval = config.getHeartbeatInterval();
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
                session::heartbeatIfIdle,
                Instant.now().plus(interval),
                interval);
        session.attachHeartbeat(task);
        return session;
    }
}
