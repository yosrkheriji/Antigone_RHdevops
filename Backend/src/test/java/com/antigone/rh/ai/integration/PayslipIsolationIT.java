package com.antigone.rh.ai.integration;

import com.antigone.rh.ai.agent.StructuredAgents;
import com.antigone.rh.ai.dto.PayslipExplanation;
import com.antigone.rh.ai.exception.AiForbiddenException;
import com.antigone.rh.ai.service.PayslipExplanationService;
import com.antigone.rh.ai.support.AbstractAiIntegrationTest;
import com.antigone.rh.ai.support.TestFixtures;
import com.antigone.rh.entity.Employe;
import com.antigone.rh.exception.ResourceNotFoundException;
import com.antigone.rh.security.AuthPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Scenarios 7 et 8 : exactitude des chiffres et etancheite entre employes.
 *
 * <p>Le test de prompt injection porte sur le controle d'acces, pas sur le prompt :
 * quelle que soit la formulation, l'identifiant cible est resolu cote serveur depuis
 * le JWT. Une tentative visant un collegue echoue donc avant toute lecture en base,
 * et le contexte transmis au modele ne contient jamais ses donnees.
 */
class PayslipIsolationIT extends AbstractAiIntegrationTest {

    @Autowired
    private PayslipExplanationService payslipService;

    @Autowired
    private TestFixtures fixtures;

    @MockitoBean
    private StructuredAgents.PayslipStructuredAgent structuredAgent;

    private Employe sarah;
    private Employe ahmed;
    private AuthPrincipal sarahPrincipal;
    private AuthPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        sarah = fixtures.employe("Gharbi", "Sarah");
        ahmed = fixtures.employe("Jebali", "Ahmed");
        sarahPrincipal = fixtures.employeePrincipal(sarah);
        adminPrincipal = fixtures.adminPrincipal(fixtures.employe("Directrice", "RH"));

        // Sarah : le net baisse de 180 DT entre juin et juillet (acompte + IRPP).
        fixtures.bulletin(sarah, "2026-06", 3000.0, 2310.0, 240.0, 270.0, 0.0);
        fixtures.bulletin(sarah, "2026-07", 3000.0, 2130.0, 300.0, 270.0, 150.0);
        // Ahmed a des montants tres differents, faciles a reperer s'ils fuitaient.
        fixtures.bulletin(ahmed, "2026-07", 9999.0, 7777.0, 1111.0, 888.0, 0.0);

        PayslipExplanation explanation = new PayslipExplanation();
        explanation.setExplanation("Votre net baisse principalement a cause d'un acompte.");
        PayslipExplanation.Comparison comparison = new PayslipExplanation.Comparison();
        comparison.setDeltaReasons(new java.util.ArrayList<>(List.of("Acompte de 150,000 DT")));
        explanation.setComparison(comparison);
        when(structuredAgent.explain(anyString())).thenReturn(explanation);
    }

    private String capturedContext() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(structuredAgent).explain(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Scenario 7 : le contexte porte les vrais chiffres des deux mois")
    void contextCarriesRealFiguresFromBothMonths() {
        payslipService.explain(sarahPrincipal, null, "2026-07");

        String context = capturedContext();
        assertThat(context)
                .contains("BULLETIN DE 2026-07")
                .contains("BULLETIN DE 2026-06")
                .contains("NET A PAYER : 2130,000 DT")
                .contains("NET A PAYER : 2310,000 DT")
                .contains("ECARTS MOIS SUR MOIS");
    }

    @Test
    @DisplayName("Scenario 7 : les ecarts sont calcules par le serveur, pas par le modele")
    void deltasAreComputedServerSide() {
        payslipService.explain(sarahPrincipal, null, "2026-07");

        String context = capturedContext();
        assertThat(context)
                .contains("Net a payer : 2310,000 -> 2130,000 (-180,000 DT)")
                .contains("IRPP mensuel : 240,000 -> 300,000 (+60,000 DT)")
                .contains("Acompte : 0,000 -> 150,000 (+150,000 DT)");
    }

    @Test
    @DisplayName("Scenario 7 : les nets exposes viennent de la base, pas de la generation")
    void exposedAmountsComeFromTheDatabase() {
        PayslipExplanation.Comparison hallucinated = new PayslipExplanation.Comparison();
        hallucinated.setPreviousNet(1.0);
        hallucinated.setCurrentNet(2.0);
        hallucinated.setDeltaReasons(new java.util.ArrayList<>());
        PayslipExplanation wrong = new PayslipExplanation();
        wrong.setExplanation("Chiffres inventes");
        wrong.setComparison(hallucinated);
        when(structuredAgent.explain(anyString())).thenReturn(wrong);

        PayslipExplanation result = payslipService.explain(sarahPrincipal, null, "2026-07");

        assertThat(result.getComparison().getCurrentNet()).isEqualTo(2130.0);
        assertThat(result.getComparison().getPreviousNet()).isEqualTo(2310.0);
        assertThat(result.getComparison().getDelta()).isEqualTo(-180.0);
    }

    @Test
    @DisplayName("Scenario 8 : viser le bulletin d'un collegue est refuse")
    void targetingAColleagueIsRejected() {
        assertThatThrownBy(() -> payslipService.explain(sarahPrincipal, ahmed.getId(), "2026-07"))
                .isInstanceOf(AiForbiddenException.class)
                .hasMessageContaining("votre propre bulletin");

        // Le refus intervient avant tout appel au modele : rien n'a pu fuiter.
        verify(structuredAgent, never()).explain(anyString());
    }

    @Test
    @DisplayName("Scenario 8 : aucune donnee d'un tiers n'entre dans le contexte")
    void noThirdPartyDataReachesTheContext() {
        payslipService.explain(sarahPrincipal, null, "2026-07");

        String context = capturedContext();
        assertThat(context).contains("Sarah");
        assertThat(context)
                .as("les montants d'Ahmed ne doivent apparaitre nulle part")
                .doesNotContain("9999")
                .doesNotContain("7777")
                .doesNotContain("Ahmed");
    }

    @Test
    @DisplayName("Scenario 8 : un employe passant son propre identifiant reste autorise")
    void employeeMayPassTheirOwnId() {
        PayslipExplanation result = payslipService.explain(sarahPrincipal, sarah.getId(), "2026-07");

        assertThat(result.getComparison().getCurrentNet()).isEqualTo(2130.0);
    }

    @Test
    @DisplayName("L'administrateur consulte le bulletin de n'importe quel employe")
    void adminMayReadAnyPayslip() {
        PayslipExplanation result = payslipService.explain(adminPrincipal, ahmed.getId(), "2026-07");

        assertThat(result.getComparison().getCurrentNet()).isEqualTo(7777.0);
    }

    @Test
    @DisplayName("L'administrateur doit designer un employe explicitement")
    void adminMustNameAnEmployee() {
        assertThatThrownBy(() -> payslipService.explain(adminPrincipal, null, "2026-07"))
                .isInstanceOf(AiForbiddenException.class);
    }

    @Test
    @DisplayName("Sans bulletin du mois precedent, la comparaison est simplement absente")
    void missingPreviousMonthIsHandledGracefully() {
        Employe nouveau = fixtures.employe("Recrue", "Nouvelle");
        AuthPrincipal principal = fixtures.employeePrincipal(nouveau);
        fixtures.bulletin(nouveau, "2026-07", 2000.0, 1600.0, 100.0, 180.0, 0.0);

        PayslipExplanation result = payslipService.explain(principal, null, "2026-07");

        assertThat(result.getComparison().getCurrentNet()).isEqualTo(1600.0);
        assertThat(result.getComparison().getPreviousNet()).isNull();
        assertThat(result.getComparison().getDelta()).isNull();
        assertThat(capturedContext()).contains("Aucun bulletin anterieur");
    }

    @Test
    @DisplayName("Un mois sans bulletin se rabat sur le dernier disponible")
    void missingMonthFallsBackToLatestPayslip() {
        // Demander « ce mois-ci » alors que la paie n'est pas close est le cas
        // courant : repondre qu'il n'y a rien laisserait l'employe sans reponse
        // alors que sa derniere fiche existe.
        PayslipExplanation result = payslipService.explain(sarahPrincipal, null, "2026-12");

        assertThat(result.getComparison().getCurrentNet()).isEqualTo(2130.0);
        assertThat(capturedContext())
                .contains("le mois demande n'a pas encore de bulletin")
                .contains("BULLETIN DE 2026-07");
    }

    @Test
    @DisplayName("Un employe sans aucun bulletin renvoie 404 sans appeler le modele")
    void employeeWithoutAnyPayslipYieldsNotFound() {
        AuthPrincipal nouveau = fixtures.employeePrincipal(fixtures.employe("Sans", "Bulletin"));

        assertThatThrownBy(() -> payslipService.explain(nouveau, null, "2026-07"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(structuredAgent, never()).explain(anyString());
    }

    @Test
    @DisplayName("Le contexte detaille chaque etape du calcul du net")
    void contextBreaksDownEveryStepOfTheNet() {
        payslipService.explain(sarahPrincipal, null, "2026-07");

        // Le modele ne doit rien recalculer : chaque etape lui arrive deja chiffree.
        assertThat(capturedContext())
                .contains("COMPOSITION DU NET, ETAPE PAR ETAPE")
                .contains("1. Brut effectif")
                .contains("2. CNSS salarie = base CNSS ×")
                .contains("3. Salaire imposable = brut ajuste IRPP − CNSS")
                .contains("4. Abattement = salaire imposable ×")
                .contains("5. Revenu net imposable")
                .contains("6. Contribution de solidarite (CSS) = base ×")
                .contains("7. IRPP mensuel = bareme progressif")
                .contains("8. Net =")
                .contains("9. Net a payer");
    }

    @Test
    @DisplayName("Les taux en vigueur accompagnent les montants")
    void ratesAccompanyTheAmounts() {
        payslipService.explain(sarahPrincipal, null, "2026-07");

        // Sans les taux, l'explication ne pourrait citer que des montants bruts,
        // sans dire d'ou ils viennent.
        assertThat(capturedContext())
                .contains("TAUX EN VIGUEUR")
                .contains("CNSS salarie :")
                .contains("Contribution sociale de solidarite (CSS) :")
                .contains("Abattement sur salaire imposable :");
    }

    @Test
    @DisplayName("Le bareme IRPP en vigueur est joint au contexte")
    void irppScaleIsIncludedInTheContext() {
        payslipService.explain(sarahPrincipal, null, "2026-07");

        assertThat(capturedContext()).contains("BAREME IRPP ANNUEL, en vigueur depuis");
    }
}
