package com.antigone.rh.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Pools dédiés à l'IA. Les orchestrations longues (~60-90 s) ne doivent jamais
 * occuper les threads Tomcat qui servent le CRUD et le login.
 */
@Configuration
@Slf4j
public class AiAsyncConfig {

    public static final String AI_EXECUTOR = "aiTaskExecutor";
    public static final String AI_HEARTBEAT_SCHEDULER = "aiHeartbeatScheduler";

    /** Exécute les générations IA (une tâche = un flux SSE ouvert). */
    @Bean(name = AI_EXECUTOR)
    public ThreadPoolTaskExecutor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-gen-");
        executor.setKeepAliveSeconds(120);
        // Au-delà de la file, on refuse plutôt que d'empiler : le client reçoit
        // une erreur immédiate au lieu d'attendre un flux qui ne démarrera pas.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(90);
        executor.initialize();
        return executor;
    }

    /** Émet les heartbeats SSE pendant les phases sans token généré. */
    @Bean(name = AI_HEARTBEAT_SCHEDULER)
    public ThreadPoolTaskScheduler aiHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("ai-heartbeat-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        scheduler.initialize();
        return scheduler;
    }
}
