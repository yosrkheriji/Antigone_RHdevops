package com.antigone.rh.ai.integration;

import com.antigone.rh.ai.agent.StructuredAgents;
import com.antigone.rh.ai.dto.ReminderDraft;
import com.antigone.rh.ai.dto.ReminderResponse;
import com.antigone.rh.ai.exception.AiForbiddenException;
import com.antigone.rh.ai.service.InvoiceLateInfo;
import com.antigone.rh.ai.service.ReminderGenerationService;
import com.antigone.rh.ai.support.AbstractAiIntegrationTest;
import com.antigone.rh.ai.support.TestFixtures;
import com.antigone.rh.ai.tools.AiCallContext;
import com.antigone.rh.ai.tools.AiToolFactory;
import com.antigone.rh.ai.tools.ReminderTools;
import com.antigone.rh.entity.Client;
import com.antigone.rh.entity.Facture;
import com.antigone.rh.entity.RelanceClient;
import com.antigone.rh.enums.StatutFacture;
import com.antigone.rh.repository.ClientRepository;
import com.antigone.rh.repository.RelanceClientRepository;
import com.antigone.rh.security.AuthPrincipal;
import com.antigone.rh.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Scenarios 4, 5 et 6 : paliers de ton, RBAC et validation humaine avant envoi.
 *
 * <p>Ce qui est verifie cote serveur, c'est le palier transmis au modele et le fait
 * qu'aucun email ne parte sans autorisation explicite — pas le style du texte
 * genere, qui n'est pas deterministe.
 */
class ReminderIT extends AbstractAiIntegrationTest {

    @Autowired
    private ReminderGenerationService reminderService;

    @Autowired
    private RelanceClientRepository relanceRepository;

    @Autowired
    private com.antigone.rh.repository.FactureRepository factureRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private TestFixtures fixtures;

    @Autowired
    private AiToolFactory toolFactory;

    @MockitoBean
    private StructuredAgents.ReminderStructuredAgent structuredAgent;

    @MockitoBean
    private EmailService emailService;

    private Client client;
    private AuthPrincipal admin;
    private AuthPrincipal socialMedia;

    @BeforeEach
    void setUp() {
        client = fixtures.client("Gamma " + System.nanoTime());
        admin = fixtures.adminPrincipal(fixtures.employe("Comptable", "Leila"));
        socialMedia = fixtures.socialMediaPrincipal(fixtures.employe("Community", "Manager"));

        ReminderDraft draft = new ReminderDraft();
        draft.setSubject("Rappel : facture en attente");
        draft.setBody("Madame,\n\nSauf erreur de notre part...\n\nL'equipe Antigone");
        when(structuredAgent.draft(anyString())).thenReturn(draft);
    }

    private String capturedContext() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(structuredAgent).draft(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Scenario 4 : trois jours de retard donnent le palier amical")
    void threeDaysLateYieldsSoftTone() {
        Facture facture = fixtures.facture(client, "FAC-S4-" + System.nanoTime(),
                LocalDate.now().minusDays(3), 1200.0, 0.0);

        ReminderGenerationService.Result result = reminderService.generate(admin, facture.getId());

        assertThat(result.tone()).isEqualTo(InvoiceLateInfo.Tone.SOFT);
        assertThat(result.joursDeRetard()).isEqualTo(3);
        assertThat(capturedContext()).contains("Palier de ton impose : SOFT");
    }

    @Test
    @DisplayName("Scenario 5 : quarante-cinq jours de retard donnent le palier formel")
    void fortyFiveDaysLateYieldsFormalTone() {
        Facture facture = fixtures.facture(client, "FAC-S5-" + System.nanoTime(),
                LocalDate.now().minusDays(45), 3400.0, 400.0);

        ReminderGenerationService.Result result = reminderService.generate(admin, facture.getId());

        assertThat(result.tone()).isEqualTo(InvoiceLateInfo.Tone.FORMAL);
        assertThat(result.joursDeRetard()).isEqualTo(45);
        assertThat(result.resteDu()).isEqualTo(3000.0);
        assertThat(capturedContext())
                .contains("Palier de ton impose : FORMAL")
                .contains("Reste du : 3000,000 DT");
    }

    @Test
    @DisplayName("Scenario 6 : un profil social media est refuse")
    void socialMediaAccountIsRejected() {
        Facture facture = fixtures.facture(client, "FAC-S6-" + System.nanoTime(),
                LocalDate.now().minusDays(10), 500.0, 0.0);

        assertThatThrownBy(() -> reminderService.generate(socialMedia, facture.getId()))
                .isInstanceOf(AiForbiddenException.class)
                .hasMessageContaining("administrateurs");

        verify(structuredAgent, never()).draft(anyString());
    }

    @Test
    @DisplayName("La generation produit un brouillon, sans envoyer quoi que ce soit")
    void generationProducesADraftWithoutSending() {
        Facture facture = fixtures.facture(client, "FAC-D-" + System.nanoTime(),
                LocalDate.now().minusDays(12), 900.0, 0.0);

        ReminderGenerationService.Result result = reminderService.generate(admin, facture.getId());

        assertThat(result.sent()).isFalse();
        assertThat(result.relanceId()).isNotNull();
        assertThat(result.subject()).isEqualTo("Rappel : facture en attente");
        verify(emailService, never()).sendHtml(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Le brouillon redige en conversation libre depose un resultat structure pour le widget")
    void conversationalDraftExposesAStructuredResultForTheWidget() {
        // Chemin distinct de reminderService.generate() : c'est celui que l'agent
        // conversationnel emprunte reellement (EmailDraftTool, appele librement par le
        // modele), pas l'endpoint dedie. Le widget n'affiche le bouton d'envoi que si
        // ce chemin depose un resultat structure dans le AiCallContext du tour.
        Facture facture = fixtures.facture(client, "FAC-W-" + System.nanoTime(),
                LocalDate.now().minusDays(5), 1667.0, 0.0);

        AiCallContext context = AiCallContext.of(admin, 1L);
        ReminderTools tools = toolFactory.reminderTools(context);

        tools.emailDraft(facture.getId(), "Rappel de paiement",
                "Madame, Monsieur,\n\nNous revenons vers vous au sujet du paiement en attente.\n\nCordialement,");

        Object structured = context.structuredResult().get();
        assertThat(structured).isInstanceOf(ReminderResponse.class);
        ReminderResponse response = (ReminderResponse) structured;

        assertThat(response.isSent()).isFalse();
        assertThat(response.getReminderId()).isNotNull();
        assertThat(response.getSubject()).isEqualTo("Rappel de paiement");
        assertThat(response.getHtmlPreview())
                .contains("<!DOCTYPE html>")
                .contains("#683b77");

        RelanceClient persisted = relanceRepository.findById(response.getReminderId()).orElseThrow();
        assertThat(persisted.getEnvoyee()).isFalse();
        assertThat(persisted.getObjet()).isEqualTo("Rappel de paiement");
    }

    @Test
    @DisplayName("Une confirmation en conversation declenche un envoi reel, identique au bouton")
    void conversationalConfirmationSendsTheEmailForReal() {
        Facture facture = fixtures.facture(client, "FAC-CC-" + System.nanoTime(),
                LocalDate.now().minusDays(12), 900.0, 0.0);
        ReminderGenerationService.Result draft = reminderService.generate(admin, facture.getId());

        AiCallContext context = AiCallContext.of(admin, 1L);
        ReminderTools tools = toolFactory.reminderTools(context);

        String outcome = tools.confirmAndSend(draft.relanceId());

        assertThat(outcome).contains("envoye avec succes");
        verify(emailService).sendHtml(anyString(), anyString(), anyString(), anyString());

        Object structured = context.structuredResult().get();
        assertThat(structured).isInstanceOf(ReminderResponse.class);
        assertThat(((ReminderResponse) structured).isSent()).isTrue();

        RelanceClient persisted = relanceRepository.findById(draft.relanceId()).orElseThrow();
        assertThat(persisted.getEnvoyee()).isTrue();
    }

    @Test
    @DisplayName("Le brouillon valide est expedie, avec le texte exact qui a ete relu")
    void confirmedDraftIsSentVerbatim() {
        Facture facture = fixtures.facture(client, "FAC-E-" + System.nanoTime(),
                LocalDate.now().minusDays(12), 900.0, 0.0);
        ReminderGenerationService.Result draft = reminderService.generate(admin, facture.getId());

        ReminderGenerationService.Result sent = reminderService.send(admin, draft.relanceId());

        assertThat(sent.sent()).isTrue();

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendHtml(anyString(), subject.capture(), html.capture(), anyString());

        // Le texte envoye est celui du brouillon, pas une regeneration : l'utilisateur
        // doit recevoir exactement ce qu'il a relu.
        assertThat(subject.getValue()).isEqualTo(draft.subject());
        assertThat(html.getValue()).contains("Sauf erreur de notre part");
    }

    @Test
    @DisplayName("L'email expedie reprend la charte de l'application")
    void sentEmailUsesTheApplicationBranding() {
        Facture facture = fixtures.facture(client, "FAC-H-" + System.nanoTime(),
                LocalDate.now().minusDays(12), 1500.0, 0.0);
        ReminderGenerationService.Result draft = reminderService.generate(admin, facture.getId());

        reminderService.send(admin, draft.relanceId());

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendHtml(anyString(), anyString(), html.capture(), anyString());

        assertThat(html.getValue())
                .contains("<!DOCTYPE html>")
                .contains("Antigone")
                // Violet de la charte, comme les autres emails de l'application.
                .contains("#683b77")
                .contains("Montant dû");
    }

    @Test
    @DisplayName("Le logo du client destinataire apparait dans l'email quand il en a un")
    void clientLogoAppearsInTheEmailWhenItHasOne() {
        client.setLogoPath("logos/gamma-" + System.nanoTime() + ".png");
        clientRepository.save(client);
        Facture facture = fixtures.facture(client, "FAC-LOGO-" + System.nanoTime(),
                LocalDate.now().minusDays(12), 900.0, 0.0);
        ReminderGenerationService.Result draft = reminderService.generate(admin, facture.getId());

        reminderService.send(admin, draft.relanceId());

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendHtml(anyString(), anyString(), html.capture(), anyString());

        assertThat(html.getValue())
                .contains("<img src=")
                .contains("/api/clients/" + client.getId() + "/logo")
                .contains(client.getNom());
    }

    @Test
    @DisplayName("Sans logo enregistre, l'email reste personnalise au nom du client sans image cassee")
    void personalizationDegradesGracefullyWithoutALogo() {
        // Le client du setUp n'a pas de logo : c'est le cas courant, il ne doit pas
        // produire de balise <img> pointant vers une ressource absente.
        Facture facture = fixtures.facture(client, "FAC-NOLOGO-" + System.nanoTime(),
                LocalDate.now().minusDays(12), 900.0, 0.0);
        ReminderGenerationService.Result draft = reminderService.generate(admin, facture.getId());

        reminderService.send(admin, draft.relanceId());

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendHtml(anyString(), anyString(), html.capture(), anyString());

        assertThat(html.getValue())
                .doesNotContain("<img src=")
                .contains(client.getNom());
    }

    @Test
    @DisplayName("Une relance deja envoyee n'est pas expediee une seconde fois")
    void alreadySentReminderIsNotSentAgain() {
        Facture facture = fixtures.facture(client, "FAC-R-" + System.nanoTime(),
                LocalDate.now().minusDays(12), 900.0, 0.0);
        ReminderGenerationService.Result draft = reminderService.generate(admin, facture.getId());
        reminderService.send(admin, draft.relanceId());

        // Le client recevrait deux fois le meme rappel.
        ReminderGenerationService.Result again = reminderService.send(admin, draft.relanceId());

        assertThat(again.sent()).isTrue();
        verify(emailService, times(1)).sendHtml(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Un profil social media ne peut pas expedier de relance")
    void socialMediaCannotSend() {
        Facture facture = fixtures.facture(client, "FAC-S-" + System.nanoTime(),
                LocalDate.now().minusDays(12), 900.0, 0.0);
        ReminderGenerationService.Result draft = reminderService.generate(admin, facture.getId());

        assertThatThrownBy(() -> reminderService.send(socialMedia, draft.relanceId()))
                .isInstanceOf(AiForbiddenException.class);
    }

    @Test
    @DisplayName("La relance est historisee dans la table metier existante")
    void reminderIsTracedInTheBusinessTable() {
        Facture facture = fixtures.facture(client, "FAC-T-" + System.nanoTime(),
                LocalDate.now().minusDays(20), 1500.0, 0.0);

        reminderService.generate(admin, facture.getId());

        var relances = relanceRepository.findByFactureIdOrderByDateRelanceDesc(facture.getId());
        assertThat(relances).hasSize(1);
        assertThat(relances.get(0).getEnvoyee()).isFalse();
        assertThat(relances.get(0).getNote())
                .contains("FIRM")
                .contains("Rappel : facture en attente");
    }

    @Test
    @DisplayName("Une facture soldee ne donne lieu a aucune relance")
    void paidInvoiceIsRejected() {
        Facture facture = fixtures.facture(client, "FAC-P-" + System.nanoTime(),
                LocalDate.now().minusDays(20), 1000.0, 1000.0);
        // Le service relit la facture : le statut doit etre en base, pas seulement
        // pose sur l'instance detachee du test.
        facture.setStatut(StatutFacture.PAYEE);
        factureRepository.save(facture);

        assertThatThrownBy(() -> reminderService.generate(admin, facture.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("soldee");
    }

    @Test
    @DisplayName("Le contexte transmis porte les montants exacts a citer")
    void contextCarriesExactAmounts() {
        Facture facture = fixtures.facture(client, "FAC-M-" + System.nanoTime(),
                LocalDate.now().minusDays(9), 2400.0, 900.0);

        reminderService.generate(admin, facture.getId());

        assertThat(capturedContext())
                .contains("Montant TTC : 2400,000 DT")
                .contains("Deja regle : 900,000 DT")
                .contains("Reste du : 1500,000 DT")
                .contains("civilite : Madame");
    }
}
