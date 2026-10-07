package com.antigone.rh.ai.unit;

import com.antigone.rh.ai.config.AiProperties;
import com.antigone.rh.ai.service.InvoiceLateInfo;
import com.antigone.rh.entity.Client;
import com.antigone.rh.entity.Facture;
import com.antigone.rh.enums.StatutFacture;
import com.antigone.rh.enums.TypeDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenarios 4 et 5 : le palier de ton decoule des jours de retard.
 *
 * <p>Cette regle est verifiee ici et non sur la sortie du LLM. Assertionner le ton
 * d'un texte genere serait fragile et non deterministe ; ce qui doit etre garanti,
 * c'est que le palier transmis au modele est le bon — le prompt, lui, impose de le
 * respecter.
 */
class InvoiceLateInfoTest {

    private final AiProperties.Reminder config = new AiProperties.Reminder();

    private Facture facture(LocalDate echeance, double ttc, double paye) {
        return Facture.builder()
                .id(1L)
                .numero("FAC-2026-0042")
                .type(TypeDocument.FACTURE)
                .client(Client.builder()
                        .id(7L)
                        .nom("Marque Alpha")
                        .email("compta@alpha.tn")
                        .emailReceiverNom("Sonia Ben Ali")
                        .emailReceiverGenre("F")
                        .build())
                .dateEmission(echeance.minusDays(30))
                .dateEcheance(echeance)
                .totalTtc(ttc)
                .montantPaye(paye)
                .statut(StatutFacture.EN_ATTENTE)
                .build();
    }

    @ParameterizedTest(name = "{0} jours de retard -> {1}")
    @CsvSource({
            "0,  SOFT",
            "3,  SOFT",
            "7,  SOFT",
            "8,  FIRM",
            "20, FIRM",
            "30, FIRM",
            "31, FORMAL",
            "45, FORMAL",
            "120,FORMAL"
    })
    @DisplayName("Le palier suit strictement les bornes configurees")
    void toneFollowsConfiguredThresholds(long joursDeRetard, InvoiceLateInfo.Tone expected) {
        LocalDate today = LocalDate.of(2026, 8, 26);
        LocalDate echeance = today.minusDays(joursDeRetard);

        InvoiceLateInfo info = InvoiceLateInfo.from(facture(echeance, 1200.0, 0.0), config, today);

        assertThat(info.joursDeRetard()).isEqualTo(joursDeRetard);
        assertThat(info.tone()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Scenario 4 : 3 jours de retard donnent le palier amical")
    void softTierAtThreeDays() {
        LocalDate today = LocalDate.of(2026, 8, 26);

        InvoiceLateInfo info = InvoiceLateInfo.from(facture(today.minusDays(3), 900.0, 0.0), config, today);

        assertThat(info.tone()).isEqualTo(InvoiceLateInfo.Tone.SOFT);
        assertThat(info.toPromptBlock()).contains("Palier de ton impose : SOFT");
    }

    @Test
    @DisplayName("Scenario 5 : 45 jours de retard donnent le palier formel")
    void formalTierAtFortyFiveDays() {
        LocalDate today = LocalDate.of(2026, 8, 26);

        InvoiceLateInfo info = InvoiceLateInfo.from(facture(today.minusDays(45), 900.0, 0.0), config, today);

        assertThat(info.tone()).isEqualTo(InvoiceLateInfo.Tone.FORMAL);
        assertThat(info.toPromptBlock()).contains("Palier de ton impose : FORMAL");
    }

    @Test
    @DisplayName("Une facture non echue n'est pas comptee en retard")
    void futureDueDateIsNotLate() {
        LocalDate today = LocalDate.of(2026, 8, 26);

        InvoiceLateInfo info = InvoiceLateInfo.from(facture(today.plusDays(10), 500.0, 0.0), config, today);

        assertThat(info.joursDeRetard()).isZero();
        assertThat(info.tone()).isEqualTo(InvoiceLateInfo.Tone.SOFT);
    }

    @Test
    @DisplayName("Le reste du tient compte des acomptes deja verses")
    void remainingAmountAccountsForPartialPayment() {
        LocalDate today = LocalDate.of(2026, 8, 26);

        InvoiceLateInfo info = InvoiceLateInfo.from(facture(today.minusDays(15), 1200.0, 450.0), config, today);

        assertThat(info.resteDu()).isEqualTo(750.0);
        assertThat(info.toPromptBlock()).contains("Reste du : 750,000 DT");
    }

    @Test
    @DisplayName("Un trop-percu ne produit jamais un reste du negatif")
    void overpaymentNeverYieldsNegativeBalance() {
        LocalDate today = LocalDate.of(2026, 8, 26);

        InvoiceLateInfo info = InvoiceLateInfo.from(facture(today.minusDays(5), 100.0, 150.0), config, today);

        assertThat(info.resteDu()).isZero();
    }

    @Test
    @DisplayName("La civilite suit le genre du destinataire, neutre par defaut")
    void salutationFollowsRecipientGender() {
        LocalDate today = LocalDate.of(2026, 8, 26);
        Facture f = facture(today.minusDays(5), 100.0, 0.0);

        assertThat(InvoiceLateInfo.from(f, config, today).civilite()).isEqualTo("Madame");

        f.getClient().setEmailReceiverGenre("M");
        assertThat(InvoiceLateInfo.from(f, config, today).civilite()).isEqualTo("Monsieur");

        f.getClient().setEmailReceiverGenre(null);
        assertThat(InvoiceLateInfo.from(f, config, today).civilite()).isEqualTo("Madame, Monsieur");
    }

    @Test
    @DisplayName("Les bornes de palier sont reglables sans toucher au code")
    void thresholdsAreConfigurable() {
        AiProperties.Reminder strict = new AiProperties.Reminder();
        strict.setSoftMaxDays(2);
        strict.setFirmMaxDays(10);
        LocalDate today = LocalDate.of(2026, 8, 26);

        InvoiceLateInfo info = InvoiceLateInfo.from(facture(today.minusDays(5), 100.0, 0.0), strict, today);

        assertThat(info.tone()).isEqualTo(InvoiceLateInfo.Tone.FIRM);
    }

    @Test
    @DisplayName("Le bloc de faits porte les valeurs reelles a citer")
    void promptBlockCarriesGroundTruth() {
        LocalDate today = LocalDate.of(2026, 8, 26);

        String block = InvoiceLateInfo.from(facture(today.minusDays(12), 2400.0, 400.0), config, today)
                .toPromptBlock();

        assertThat(block)
                .contains("Facture : FAC-2026-0042")
                .contains("Client : Marque Alpha")
                .contains("Montant TTC : 2400,000 DT")
                .contains("Deja regle : 400,000 DT")
                .contains("Reste du : 2000,000 DT")
                .contains("Jours de retard : 12");
    }
}
