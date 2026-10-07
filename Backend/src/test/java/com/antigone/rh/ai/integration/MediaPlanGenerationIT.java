package com.antigone.rh.ai.integration;

import com.antigone.rh.ai.agent.StructuredAgents;
import com.antigone.rh.ai.dto.GeneratedMediaPlan;
import com.antigone.rh.ai.dto.GeneratedMediaPlanItem;
import com.antigone.rh.ai.exception.AiForbiddenException;
import com.antigone.rh.ai.service.DriveProvisioningService;
import com.antigone.rh.ai.service.MediaPlanGenerationService;
import com.antigone.rh.ai.support.AbstractAiIntegrationTest;
import com.antigone.rh.ai.support.TestFixtures;
import com.antigone.rh.entity.Client;
import com.antigone.rh.entity.Employe;
import com.antigone.rh.entity.MediaPlan;
import com.antigone.rh.repository.MediaPlanRepository;
import com.antigone.rh.security.AuthPrincipal;
import com.antigone.rh.service.GoogleDriveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Scenarios 1, 2, 3 et 12 : pipeline de generation de media plan.
 *
 * <p>Le LLM est double. Ce que garantit un test n'est donc pas que le modele evite
 * les repetitions — cela depend de lui — mais que <em>l'historique lui est
 * effectivement fourni</em>, assorti de la consigne de non-repetition. C'est la
 * partie deterministe, et la seule qu'une regression puisse casser silencieusement.
 */
class MediaPlanGenerationIT extends AbstractAiIntegrationTest {

    @Autowired
    private MediaPlanGenerationService generationService;

    @Autowired
    private MediaPlanRepository mediaPlanRepository;

    @Autowired
    private TestFixtures fixtures;

    @Autowired
    private DriveProvisioningService driveProvisioning;

    @MockitoBean
    private StructuredAgents.MediaPlanStructuredAgent structuredAgent;

    @MockitoBean
    private GoogleDriveService googleDriveService;

    private static final YearMonth TARGET = YearMonth.of(2026, 7);

    private Employe employe;
    private Client client;
    private AuthPrincipal principal;

    @BeforeEach
    void setUp() throws IOException {
        employe = fixtures.employe("Bensalem", "Nadia");
        client = fixtures.client("Alpha " + System.nanoTime());
        fixtures.assign(employe, client);
        principal = fixtures.socialMediaPrincipal(employe);

        when(googleDriveService.getOrCreateClientMonthFolder(anyString(), any()))
                .thenReturn("https://drive.google.com/drive/folders/juillet2026");
        when(structuredAgent.generate(anyString())).thenReturn(plan(
                item("2026-07-03", "Nouveau rituel du matin", "Instagram", "Reel"),
                item("2026-07-11", "Guide des matieres naturelles", "Instagram", "Carrousel"),
                item("2026-07-19", "Temoignage cliente", "Facebook", "Video")));
    }

    private GeneratedMediaPlanItem item(String date, String titre, String plateforme, String format) {
        GeneratedMediaPlanItem generated = new GeneratedMediaPlanItem();
        generated.setDate(date);
        generated.setHeure("09:30");
        generated.setTitre(titre);
        generated.setTexteSurVisuel("Texte de " + titre);
        generated.setInspiration("Inspiration de " + titre);
        generated.setAutresElements("#antigone");
        generated.setPlatforme(plateforme);
        generated.setFormat(format);
        generated.setType("Brand content");
        generated.setJustification("Sert l'objectif de notoriete");
        return generated;
    }

    private GeneratedMediaPlan plan(GeneratedMediaPlanItem... items) {
        GeneratedMediaPlan generated = new GeneratedMediaPlan();
        generated.setSyntheseEditoriale("Mois centre sur l'usage produit.");
        generated.setThematiquesEvitees(List.of("Coulisses de l atelier", "Portrait d artisanes"));
        generated.setPublications(new java.util.ArrayList<>(List.of(items)));
        return generated;
    }

    private String capturedContext() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(structuredAgent).generate(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Scenario 1 : l'historique des 3 mois precedents est injecte au modele")
    void previousThreeMonthsAreInjectedIntoTheContext() {
        fixtures.historique(client, employe, TARGET.atDay(1));

        generationService.generate(principal, client.getId(), TARGET.toString(),
                MediaPlanGenerationService.ProgressListener.NOOP);

        String context = capturedContext();
        assertThat(context)
                .contains("Coulisses de l atelier de couture")
                .contains("Portrait de nos artisanes")
                .contains("Lancement de la collection printemps")
                .contains("DEJA UTILISES");
    }

    @Test
    @DisplayName("Scenario 1 : chaque publication generee porte un lienDrive")
    void everyGeneratedItemGetsADriveLink() {
        fixtures.historique(client, employe, TARGET.atDay(1));

        MediaPlanGenerationService.Result result = generationService.generate(
                principal, client.getId(), TARGET.toString(),
                MediaPlanGenerationService.ProgressListener.NOOP);

        assertThat(result.publications()).hasSize(3);
        assertThat(result.drivePending()).isFalse();
        assertThat(result.publications())
                .allSatisfy(plan -> assertThat(plan.getLienDrive())
                        .isEqualTo("https://drive.google.com/drive/folders/juillet2026"));
    }

    @Test
    @DisplayName("Scenario 1 : les lignes sont persistees sur le bon mois et le bon client")
    void generatedItemsArePersistedForTheRightClientAndMonth() {
        generationService.generate(principal, client.getId(), TARGET.toString(),
                MediaPlanGenerationService.ProgressListener.NOOP);

        List<MediaPlan> stored = mediaPlanRepository.findByClientId(client.getId());

        assertThat(stored).hasSize(3);
        assertThat(stored).allSatisfy(plan -> {
            assertThat(YearMonth.from(plan.getDatePublication())).isEqualTo(TARGET);
            assertThat(plan.getClient().getId()).isEqualTo(client.getId());
            assertThat(plan.getCreateur().getId()).isEqualTo(employe.getId());
            assertThat(plan.getStatut().name()).isEqualTo("EN_ATTENTE");
            assertThat(plan.getEtatPublication().name()).isEqualTo("PAS_ENCORE");
        });
        // La justification editoriale est rangee dans remarques, donc visible depuis
        // l'ecran media plan existant.
        assertThat(stored).allSatisfy(plan ->
                assertThat(plan.getRemarques()).isEqualTo("Sert l'objectif de notoriete"));
    }

    @Test
    @DisplayName("Scenario 2 : une marque sans historique se genere sans planter")
    void brandWithoutHistoryStillGenerates() {
        MediaPlanGenerationService.Result result = generationService.generate(
                principal, client.getId(), TARGET.toString(),
                MediaPlanGenerationService.ProgressListener.NOOP);

        assertThat(result.publications()).hasSize(3);

        String context = capturedContext();
        assertThat(context)
                .contains("Aucun historique")
                .contains("Identite")
                .contains("Objectifs");
    }

    @Test
    @DisplayName("Scenario 3 : une marque hors perimetre est refusee")
    void foreignBrandIsRejected() {
        Client autreMarque = fixtures.client("Beta " + System.nanoTime());

        assertThatThrownBy(() -> generationService.generate(
                principal, autreMarque.getId(), TARGET.toString(),
                MediaPlanGenerationService.ProgressListener.NOOP))
                .isInstanceOf(AiForbiddenException.class)
                .hasMessageContaining("perimetre");

        assertThat(mediaPlanRepository.findByClientId(autreMarque.getId())).isEmpty();
    }

    @Test
    @DisplayName("Scenario 3 : un compte sans permission media plan est refuse")
    void accountWithoutMediaPlanPermissionIsRejected() {
        AuthPrincipal plainEmployee = fixtures.employeePrincipal(fixtures.employe("Trabelsi", "Karim"));

        assertThatThrownBy(() -> generationService.generate(
                plainEmployee, client.getId(), TARGET.toString(),
                MediaPlanGenerationService.ProgressListener.NOOP))
                .isInstanceOf(AiForbiddenException.class)
                .hasMessageContaining("VIEW_MEDIA_PLAN");
    }

    @Test
    @DisplayName("Scenario 12 : Drive en panne, le plan est quand meme persiste en PENDING")
    void driveFailureStillPersistsThePlanAsPending() throws IOException {
        when(googleDriveService.getOrCreateClientMonthFolder(anyString(), any()))
                .thenThrow(new IOException("503 Drive indisponible"));

        MediaPlanGenerationService.Result result = generationService.generate(
                principal, client.getId(), TARGET.toString(),
                MediaPlanGenerationService.ProgressListener.NOOP);

        assertThat(result.drivePending()).isTrue();
        assertThat(result.publications()).hasSize(3);
        assertThat(mediaPlanRepository.findByClientId(client.getId()))
                .allSatisfy(plan -> assertThat(plan.getLienDrive())
                        .isEqualTo(DriveProvisioningService.LIEN_DRIVE_PENDING));
    }

    @Test
    @DisplayName("Scenario 12 : la reprise cible l'etape Drive sans relancer la generation")
    void driveRetryDoesNotRegenerate() throws IOException {
        when(googleDriveService.getOrCreateClientMonthFolder(anyString(), any()))
                .thenThrow(new IOException("indisponible"));
        generationService.generate(principal, client.getId(), TARGET.toString(),
                MediaPlanGenerationService.ProgressListener.NOOP);

        // Drive revient : seule l'etape d'approvisionnement est rejouee.
        org.mockito.Mockito.reset(googleDriveService);
        when(googleDriveService.getOrCreateClientMonthFolder(anyString(), any()))
                .thenReturn("https://drive.google.com/drive/folders/reprise");

        // Le bean Spring, pas une instance construite a la main : retryPending lit
        // MediaPlan.client qui est LAZY, ce qui exige la transaction du proxy.
        int updated = driveProvisioning.retryPending(client.getId(), TARGET.toString());

        assertThat(updated).isEqualTo(3);
        assertThat(mediaPlanRepository.findByClientId(client.getId()))
                .allSatisfy(plan -> assertThat(plan.getLienDrive())
                        .isEqualTo("https://drive.google.com/drive/folders/reprise"));
        // Aucun second appel au modele : la generation n'a pas ete refaite.
        org.mockito.Mockito.verify(structuredAgent, org.mockito.Mockito.times(1)).generate(anyString());
    }

    @Test
    @DisplayName("Une date hors du mois demande est recalee plutot que rejetee")
    void outOfMonthDateIsRealignedInsteadOfLost() {
        when(structuredAgent.generate(anyString())).thenReturn(plan(
                item("2026-09-15", "Date hors mois", "Instagram", "Reel"),
                item("pas-une-date", "Date illisible", "Instagram", "Reel")));

        MediaPlanGenerationService.Result result = generationService.generate(
                principal, client.getId(), TARGET.toString(),
                MediaPlanGenerationService.ProgressListener.NOOP);

        assertThat(result.publications()).hasSize(2);
        assertThat(result.publications())
                .allSatisfy(plan -> assertThat(YearMonth.from(plan.getDatePublication())).isEqualTo(TARGET));
        assertThat(result.publications().get(0).getDatePublication()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(result.publications().get(1).getDatePublication()).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @Test
    @DisplayName("Un mois mal forme est refuse avant tout appel au modele")
    void malformedMonthIsRejectedBeforeCallingTheModel() {
        assertThatThrownBy(() -> generationService.generate(
                principal, client.getId(), "juillet-2026",
                MediaPlanGenerationService.ProgressListener.NOOP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("YYYY-MM");

        org.mockito.Mockito.verify(structuredAgent, org.mockito.Mockito.never()).generate(anyString());
    }

    @Test
    @DisplayName("Les projets actifs sur la periode sont signales au modele")
    void activeProjectsAreFlaggedInTheContext() {
        fixtures.projet(client, "Campagne ete", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31));
        fixtures.projet(client, "Refonte site", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30));

        generationService.generate(principal, client.getId(), TARGET.toString(),
                MediaPlanGenerationService.ProgressListener.NOOP);

        String context = capturedContext();
        assertThat(context).contains("Campagne ete").contains("actif sur le mois demande");
        assertThat(context).contains("Refonte site");
        assertThat(context.substring(context.indexOf("Refonte site")))
                .doesNotContain("actif sur le mois demande");
    }

    @Test
    @DisplayName("Les etapes du pipeline sont rapportees dans l'ordre du cahier des charges")
    void pipelineStepsAreReportedInSpecifiedOrder() {
        List<String> steps = new java.util.ArrayList<>();
        generationService.generate(principal, client.getId(), TARGET.toString(),
                new MediaPlanGenerationService.ProgressListener() {
                    @Override
                    public void start(MediaPlanGenerationService.Step step, Object arguments) {
                        steps.add(step.name());
                    }

                    @Override
                    public void end(MediaPlanGenerationService.Step step, boolean success, String detail) {
                    }
                });

        assertThat(steps).containsExactly(
                "BRAND", "PROJECTS", "HISTORY", "CONTENT", "GENERATION", "DRIVE", "PERSIST");
    }
}
