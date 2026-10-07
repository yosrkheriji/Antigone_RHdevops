package com.antigone.rh.ai.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Un flux SSE de generation, avec son heartbeat et sa detection de deconnexion.
 *
 * <p>Contrat d'evenements emis au frontend :
 * <ul>
 *   <li>{@code token} — fragment de texte genere par le LLM ;</li>
 *   <li>{@code tool_call_start} / {@code tool_call_end} — progression des outils,
 *       ce qui permet d'afficher « Creation du dossier Drive... » plutot qu'un
 *       spinner fige pendant une minute ;</li>
 *   <li>{@code heartbeat} — maintien de connexion pendant les phases sans token ;</li>
 *   <li>{@code structured_result} — resultat structure final, emis une seule fois ;</li>
 *   <li>{@code error} — echec, avec code et message ;</li>
 *   <li>{@code done} — toujours emis en dernier.</li>
 * </ul>
 *
 * <p>Toutes les emissions sont serialisees sur le moniteur de l'instance : le
 * heartbeat s'execute sur un scheduler distinct du thread de generation, et
 * {@code SseEmitter} n'est pas thread-safe.
 */
@Slf4j
public class AiSseSession implements AutoCloseable {

    public static final String EVENT_TOKEN = "token";
    public static final String EVENT_TOOL_START = "tool_call_start";
    public static final String EVENT_TOOL_END = "tool_call_end";
    public static final String EVENT_HEARTBEAT = "heartbeat";
    public static final String EVENT_STRUCTURED = "structured_result";
    public static final String EVENT_ERROR = "error";
    public static final String EVENT_DONE = "done";

    private final SseEmitter emitter;
    private final ObjectMapper objectMapper;
    private final Duration heartbeatInterval;

    private final AtomicBoolean finished = new AtomicBoolean(false);
    /** Passe a vrai des que le client se deconnecte ou que le flux expire. */
    private final AtomicBoolean disconnected = new AtomicBoolean(false);
    private final AtomicLong lastEmitAt = new AtomicLong(System.currentTimeMillis());

    private volatile ScheduledFuture<?> heartbeatTask;

    AiSseSession(SseEmitter emitter, ObjectMapper objectMapper, Duration heartbeatInterval) {
        this.emitter = emitter;
        this.objectMapper = objectMapper;
        this.heartbeatInterval = heartbeatInterval;
        emitter.onCompletion(() -> markDisconnected("completion"));
        emitter.onTimeout(() -> markDisconnected("timeout"));
        emitter.onError(throwable -> markDisconnected("error: " + throwable.getMessage()));
    }

    public SseEmitter emitter() {
        return emitter;
    }

    /**
     * Vrai si le client n'ecoute plus. La generation continue malgre tout jusqu'a
     * son terme pour que le message final soit persiste : une deconnexion cote
     * client ne doit pas faire perdre le travail deja paye au LLM.
     */
    public boolean isDisconnected() {
        return disconnected.get();
    }

    void attachHeartbeat(ScheduledFuture<?> task) {
        this.heartbeatTask = task;
    }

    Duration heartbeatInterval() {
        return heartbeatInterval;
    }

    // ---- Emissions ---------------------------------------------------------

    public void token(String delta) {
        if (delta == null || delta.isEmpty()) {
            return;
        }
        send(EVENT_TOKEN, Map.of("delta", delta));
    }

    public void toolCallStart(String tool, Object arguments) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tool", tool);
        payload.put("args", arguments == null ? Map.of() : arguments);
        send(EVENT_TOOL_START, payload);
    }

    public void toolCallEnd(String tool, boolean success, String detail) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tool", tool);
        payload.put("status", success ? "success" : "error");
        if (detail != null) {
            payload.put("detail", detail);
        }
        send(EVENT_TOOL_END, payload);
    }

    public void structuredResult(Object result) {
        send(EVENT_STRUCTURED, result);
    }

    public void error(String code, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", code);
        payload.put("message", message);
        send(EVENT_ERROR, payload);
    }

    /**
     * Emis uniquement si rien n'a transite depuis l'intervalle configure : inutile
     * de doubler un flux de tokens deja actif.
     */
    void heartbeatIfIdle() {
        if (finished.get() || disconnected.get()) {
            return;
        }
        long idleMillis = System.currentTimeMillis() - lastEmitAt.get();
        if (idleMillis >= heartbeatInterval.toMillis()) {
            send(EVENT_HEARTBEAT, Map.of());
        }
    }

    /** Emet {@code done} puis ferme le flux. Idempotent. */
    public void complete() {
        if (!finished.compareAndSet(false, true)) {
            return;
        }
        cancelHeartbeat();
        if (!disconnected.get()) {
            send(EVENT_DONE, Map.of());
            try {
                emitter.complete();
            } catch (Exception e) {
                log.debug("Fermeture du flux SSE : {}", e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        complete();
    }

    // ---- Interne -----------------------------------------------------------

    private synchronized void send(String eventName, Object payload) {
        if (disconnected.get() || (finished.get() && !EVENT_DONE.equals(eventName))) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(objectMapper.writeValueAsString(payload), org.springframework.http.MediaType.APPLICATION_JSON));
            lastEmitAt.set(System.currentTimeMillis());
        } catch (IOException | IllegalStateException e) {
            // Client parti en cours de route : on cesse d'emettre, la generation
            // se poursuit et le message final sera persiste malgre tout.
            markDisconnected("send failed: " + e.getMessage());
        } catch (Exception e) {
            log.warn("Emission SSE '{}' en echec : {}", eventName, e.getMessage());
        }
    }

    private void markDisconnected(String reason) {
        if (disconnected.compareAndSet(false, true)) {
            log.debug("Flux SSE interrompu ({}) - la generation continue jusqu'a persistance", reason);
            cancelHeartbeat();
        }
    }

    private void cancelHeartbeat() {
        ScheduledFuture<?> task = heartbeatTask;
        if (task != null) {
            task.cancel(false);
        }
    }
}
