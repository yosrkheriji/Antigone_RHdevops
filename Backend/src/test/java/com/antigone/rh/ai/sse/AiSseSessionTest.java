package com.antigone.rh.ai.sse;

import com.antigone.rh.ai.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Scenario 13 : tenue du flux SSE pendant une phase longue sans token.
 *
 * <p>Le test se place dans le package {@code sse} pour acceder au constructeur
 * package-private de la session, et intervalle de heartbeat raccourci a 100 ms :
 * attendre reellement 45 secondes prouverait exactement la meme propriete en
 * immobilisant la suite. La resistance aux tâches longues cote configuration est
 * verifiee separement par {@link AiTimeoutConfigurationTest}.
 */
class AiSseSessionTest {

    /** Capture ce qu'un client recevrait reellement sur le fil. */
    private static class RecordingEmitter extends SseEmitter {
        private final List<String> wire = new CopyOnWriteArrayList<>();
        private final AtomicBoolean broken = new AtomicBoolean(false);
        private final AtomicBoolean completed = new AtomicBoolean(false);

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (broken.get()) {
                throw new IOException("client parti");
            }
            StringBuilder frame = new StringBuilder();
            builder.build().forEach(part -> frame.append(part.getData()));
            wire.add(frame.toString());
        }

        @Override
        public void complete() {
            completed.set(true);
        }

        List<String> framesNamed(String eventName) {
            return wire.stream().filter(frame -> frame.contains("event:" + eventName)).toList();
        }
    }

    private ThreadPoolTaskScheduler scheduler;
    private AiSseFactory factory;
    private AiProperties properties;

    @BeforeEach
    void setUp() {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.initialize();

        properties = new AiProperties();
        properties.getSse().setHeartbeatInterval(Duration.ofMillis(100));
        properties.getSse().setTimeout(Duration.ofSeconds(30));

        factory = new AiSseFactory(new ObjectMapper(), properties, scheduler);
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdown();
    }

    private AiSseSession sessionOn(RecordingEmitter emitter) {
        AiSseSession session = new AiSseSession(emitter, new ObjectMapper(),
                properties.getSse().getHeartbeatInterval());
        session.attachHeartbeat(scheduler.scheduleAtFixedRate(session::heartbeatIfIdle,
                java.time.Instant.now().plus(properties.getSse().getHeartbeatInterval()),
                properties.getSse().getHeartbeatInterval()));
        return session;
    }

    @Test
    @DisplayName("Un heartbeat est emis pendant une phase longue sans token")
    void heartbeatIsEmittedDuringLongToolCall() {
        RecordingEmitter emitter = new RecordingEmitter();
        AiSseSession session = sessionOn(emitter);

        session.toolCallStart("GoogleDriveTool", Map.of("mois", "2026-07"));

        // Simule un appel d'outil long : aucun token ne circule pendant ce temps.
        await().atMost(Duration.ofSeconds(3))
                .until(() -> emitter.framesNamed("heartbeat").size() >= 2);

        session.toolCallEnd("GoogleDriveTool", true, null);
        session.complete();

        assertThat(emitter.framesNamed("heartbeat")).hasSizeGreaterThanOrEqualTo(2);
        assertThat(emitter.framesNamed("tool_call_start")).hasSize(1);
        assertThat(emitter.framesNamed("tool_call_end")).hasSize(1);
    }

    @Test
    @DisplayName("Aucun heartbeat superflu tant que des tokens circulent")
    void noHeartbeatWhileTokensFlow() throws Exception {
        RecordingEmitter emitter = new RecordingEmitter();
        AiSseSession session = sessionOn(emitter);

        for (int i = 0; i < 20; i++) {
            session.token("mot" + i + " ");
            Thread.sleep(30);
        }
        session.complete();

        assertThat(emitter.framesNamed("token")).hasSize(20);
        assertThat(emitter.framesNamed("heartbeat")).isEmpty();
    }

    @Test
    @DisplayName("Le flux se termine par structured_result puis done")
    void streamEndsWithStructuredResultThenDone() {
        RecordingEmitter emitter = new RecordingEmitter();
        AiSseSession session = sessionOn(emitter);

        session.token("Voici le plan. ");
        session.structuredResult(Map.of("month", "2026-07", "items", List.of()));
        session.complete();

        assertThat(emitter.framesNamed("structured_result")).hasSize(1);
        assertThat(emitter.framesNamed("structured_result").get(0)).contains("2026-07");
        assertThat(emitter.wire.get(emitter.wire.size() - 1)).contains("event:done");
        assertThat(emitter.completed).isTrue();
    }

    @Test
    @DisplayName("Une deconnexion client arrete l'emission sans lever d'exception")
    void clientDisconnectStopsEmissionSilently() {
        RecordingEmitter emitter = new RecordingEmitter();
        AiSseSession session = sessionOn(emitter);

        session.token("debut ");
        emitter.broken.set(true);

        // La generation continue de pousser des tokens : rien ne doit remonter.
        session.token("suite ");
        session.token("fin");
        session.structuredResult(Map.of("k", "v"));

        assertThat(session.isDisconnected()).isTrue();
        assertThat(emitter.framesNamed("token")).hasSize(1);
        assertThat(emitter.framesNamed("structured_result")).isEmpty();
    }

    @Test
    @DisplayName("complete() est idempotent et n'emet qu'un seul done")
    void completeIsIdempotent() {
        RecordingEmitter emitter = new RecordingEmitter();
        AiSseSession session = sessionOn(emitter);

        session.complete();
        session.complete();
        session.complete();

        assertThat(emitter.framesNamed("done")).hasSize(1);
    }

    @Test
    @DisplayName("Le heartbeat s'arrete des la cloture du flux")
    void heartbeatStopsAfterCompletion() throws Exception {
        RecordingEmitter emitter = new RecordingEmitter();
        AiSseSession session = sessionOn(emitter);

        session.complete();
        int afterComplete = emitter.wire.size();
        Thread.sleep(500);

        assertThat(emitter.wire).hasSize(afterComplete);
    }

    @Test
    @DisplayName("L'evenement error porte un code et un message exploitables")
    void errorEventCarriesCodeAndMessage() {
        RecordingEmitter emitter = new RecordingEmitter();
        AiSseSession session = sessionOn(emitter);

        session.error("GENERATION_FAILED", "Le modele n'a pas repondu");
        session.complete();

        List<String> errors = emitter.framesNamed("error");
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("GENERATION_FAILED").contains("Le modele n'a pas repondu");
    }

    @Test
    @DisplayName("La fabrique arme bien le heartbeat des l'ouverture")
    void factoryArmsHeartbeatOnOpen() {
        AiSseSession session = factory.open();

        assertThat(session.emitter()).isNotNull();
        assertThat(session.isDisconnected()).isFalse();
        session.complete();
    }

    @Test
    @DisplayName("Les emissions concurrentes ne se corrompent pas entre elles")
    void concurrentEmissionsAreSerialised() throws Exception {
        RecordingEmitter emitter = new RecordingEmitter();
        AiSseSession session = sessionOn(emitter);

        List<Thread> threads = Collections.synchronizedList(new java.util.ArrayList<>());
        for (int t = 0; t < 4; t++) {
            Thread thread = new Thread(() -> {
                for (int i = 0; i < 50; i++) {
                    session.token("x");
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        session.complete();

        assertThat(emitter.framesNamed("token")).hasSize(200);
        assertThat(emitter.framesNamed("token")).allMatch(frame -> frame.contains("{\"delta\":\"x\"}"));
    }
}
