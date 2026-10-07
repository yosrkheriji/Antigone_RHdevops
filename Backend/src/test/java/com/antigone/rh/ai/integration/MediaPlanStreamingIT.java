package com.antigone.rh.ai.integration;

import com.antigone.rh.ai.agent.StructuredAgents;
import com.antigone.rh.ai.dto.GeneratedMediaPlan;
import com.antigone.rh.ai.dto.GeneratedMediaPlanItem;
import com.antigone.rh.ai.support.AbstractAiIntegrationTest;
import com.antigone.rh.ai.support.TestFixtures;
import com.antigone.rh.entity.Client;
import com.antigone.rh.entity.Employe;
import com.antigone.rh.repository.MediaPlanRepository;
import com.antigone.rh.security.AuthPrincipal;
import com.antigone.rh.security.JwtService;
import com.antigone.rh.service.GoogleDriveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Scenario 13 : flux SSE tenu pendant une tache longue.
 *
 * <p>Le cahier des charges parle d'un outil ralenti de 45 s. Le test utilise un
 * delai plus court avec un heartbeat abaisse a 150 ms (profil de test) : la
 * propriete verifiee est identique — des heartbeats sont bien emis pendant une phase
 * sans token, et le flux survit — mais la suite ne s'immobilise pas trois quarts de
 * minute. Que la configuration livree tienne reellement 90 s est verifie separement
 * par {@code AiTimeoutConfigurationTest}, sur le YAML de production.
 *
 * <p>{@code print = NONE} : l'impression de diagnostic de MockMvc parcourt les
 * en-tetes de la reponse simulee pendant que le thread de generation termine encore
 * l'emitter SSE. {@code MockHttpServletResponse} n'etant pas thread-safe, cela levait
 * par intermittence une {@code ConcurrentModificationException} en CI — un artefact
 * du test, absent d'un vrai conteneur de servlets. Les assertions sont inchangees.
 */
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class MediaPlanStreamingIT extends AbstractAiIntegrationTest {

    /** Assez long pour produire plusieurs heartbeats a 150 ms d'intervalle. */
    private static final long SLOW_TOOL_MILLIS = 1_200;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MediaPlanRepository mediaPlanRepository;

    @Autowired
    private TestFixtures fixtures;

    @MockitoBean
    private StructuredAgents.MediaPlanStructuredAgent structuredAgent;

    @MockitoBean
    private GoogleDriveService googleDriveService;

    private Client client;
    private String bearer;

    @BeforeEach
    void setUp() throws Exception {
        Employe employe = fixtures.employe("Streaming", "Test");
        client = fixtures.client("Delta " + System.nanoTime());
        fixtures.assign(employe, client);
        AuthPrincipal principal = fixtures.socialMediaPrincipal(employe);
        bearer = "Bearer " + jwtService.generateToken(principal);

        // Outil volontairement lent : c'est la phase pendant laquelle aucun token
        // n'est genere et ou seuls les heartbeats maintiennent la connexion.
        when(googleDriveService.getOrCreateClientMonthFolder(anyString(), any()))
                .thenAnswer(call -> {
                    Thread.sleep(SLOW_TOOL_MILLIS);
                    return "https://drive.google.com/drive/folders/lent";
                });

        GeneratedMediaPlanItem item = new GeneratedMediaPlanItem();
        item.setDate("2026-07-08");
        item.setHeure("10:00");
        item.setTitre("Publication generee");
        item.setPlatforme("Instagram");
        item.setFormat("Reel");
        item.setType("Produit");
        item.setJustification("Objectif notoriete");

        GeneratedMediaPlan plan = new GeneratedMediaPlan();
        plan.setSyntheseEditoriale("Synthese du mois");
        plan.setThematiquesEvitees(List.of("Coulisses"));
        plan.setPublications(new java.util.ArrayList<>(List.of(item)));
        when(structuredAgent.generate(anyString())).thenReturn(plan);
    }

    private String streamGeneration() throws Exception {
        MvcResult started = mockMvc.perform(post("/api/v1/media-plans/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + client.getId() + ",\"month\":\"2026-07\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        // Attend la fin du flux : l'emitter se ferme sur son evenement done.
        started.getAsyncResult(30_000);

        return mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    @DisplayName("La connexion tient pendant l'outil lent et le flux se termine normalement")
    void connectionSurvivesTheSlowToolAndCompletes() throws Exception {
        String stream = streamGeneration();

        assertThat(stream).contains("event:done");
        assertThat(stream).doesNotContain("event:error");
    }

    @Test
    @DisplayName("Des heartbeats sont emis pendant la phase sans token")
    void heartbeatsAreEmittedDuringTheSilentPhase() throws Exception {
        String stream = streamGeneration();

        long heartbeats = stream.lines().filter(line -> line.contains("event:heartbeat")).count();

        assertThat(heartbeats)
                .as("un outil de %d ms avec un heartbeat de 150 ms doit en produire plusieurs",
                        SLOW_TOOL_MILLIS)
                .isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("La progression des etapes est rapportee au frontend")
    void toolProgressIsReported() throws Exception {
        String stream = streamGeneration();

        assertThat(stream)
                .contains("event:tool_call_start")
                .contains("event:tool_call_end")
                .contains("BrandInfoTool")
                .contains("GoogleDriveTool");
    }

    @Test
    @DisplayName("Le resultat structure est streame puis persiste en base")
    void structuredResultIsStreamedThenPersisted() throws Exception {
        String stream = streamGeneration();

        assertThat(stream).contains("event:structured_result");
        assertThat(stream).contains("Publication generee");

        // Et surtout : la ligne existe reellement en base a la fin du flux.
        assertThat(mediaPlanRepository.findByClientId(client.getId()))
                .hasSize(1)
                .allSatisfy(plan -> {
                    assertThat(plan.getTitre()).isEqualTo("Publication generee");
                    assertThat(plan.getDatePublication()).isEqualTo(LocalDate.of(2026, 7, 8));
                    assertThat(plan.getLienDrive()).isEqualTo("https://drive.google.com/drive/folders/lent");
                });
    }

    @Test
    @DisplayName("L'evenement done est toujours le dernier du flux")
    void doneIsAlwaysTheLastEvent() throws Exception {
        String stream = streamGeneration();

        List<String> events = stream.lines()
                .filter(line -> line.startsWith("event:"))
                .toList();

        assertThat(events).isNotEmpty();
        assertThat(events.get(events.size() - 1)).isEqualTo("event:done");
    }

    @Test
    @DisplayName("Une marque hors perimetre est refusee avant l'ouverture du flux")
    void foreignBrandIsRejectedBeforeStreaming() throws Exception {
        Client autre = fixtures.client("Epsilon " + System.nanoTime());

        mockMvc.perform(post("/api/v1/media-plans/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + autre.getId() + ",\"month\":\"2026-07\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sans jeton, l'endpoint refuse l'acces")
    void unauthenticatedRequestIsRejected() throws Exception {
        // 403 et non 401 : l'application n'declare pas d'AuthenticationEntryPoint,
        // donc Spring Security refuse les anonymes en 403 sur tous ses endpoints.
        // Comportement preexistant, conserve pour ne pas changer la facon dont les
        // frontends deja en place interpretent l'expiration de session.
        mockMvc.perform(post("/api/v1/media-plans/generate")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"month\":\"2026-07\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un mois mal forme est rejete en 422 avec un code exploitable")
    void malformedMonthYieldsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/media-plans/generate")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + client.getId() + ",\"month\":\"juillet\"}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
