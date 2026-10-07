package com.antigone.rh.ai.service;

import com.antigone.rh.ai.config.AiProperties;
import com.antigone.rh.entity.Client;
import com.antigone.rh.entity.Facture;
import com.antigone.rh.enums.StatutFacture;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Situation d'impaye d'une facture, calculee a partir des donnees reelles.
 *
 * <p>Le palier de ton en decoule mecaniquement, il n'est jamais choisi par le LLM :
 * le modele redige, la regle metier decide de la fermete. Les bornes restent
 * configurables ({@code app.ai.reminder.*}) car elles relevent d'un arbitrage
 * commercial, pas technique.
 */
public record InvoiceLateInfo(Long factureId,
                              String numero,
                              Long clientId,
                              String clientNom,
                              String clientEmail,
                              /** Vrai si le client a un logo enregistre : conditionne son affichage dans l'email. */
                              boolean clientHasLogo,
                              String destinataireNom,
                              String destinataireGenre,
                              LocalDate dateEmission,
                              LocalDate dateEcheance,
                              double totalTtc,
                              double montantPaye,
                              double resteDu,
                              StatutFacture statut,
                              long joursDeRetard,
                              Tone tone) {

    public enum Tone {
        /** 0-7 jours : rappel doux, ton amical. */
        SOFT,
        /** 8-30 jours : rappel ferme mais courtois. */
        FIRM,
        /** Au-dela : ton formel, mention des consequences. */
        FORMAL
    }

    public static InvoiceLateInfo from(Facture facture, AiProperties.Reminder config, LocalDate today) {
        Client client = facture.getClient();
        double totalTtc = facture.getTotalTtc() == null ? 0.0 : facture.getTotalTtc();
        double paye = facture.getMontantPaye() == null ? 0.0 : facture.getMontantPaye();
        long joursDeRetard = 0;
        if (facture.getDateEcheance() != null && today.isAfter(facture.getDateEcheance())) {
            joursDeRetard = ChronoUnit.DAYS.between(facture.getDateEcheance(), today);
        }
        return new InvoiceLateInfo(
                facture.getId(),
                facture.getNumero(),
                client != null ? client.getId() : null,
                client != null ? client.getNom() : null,
                client != null ? firstNonBlank(client.getEmail(), client.getContactEmail()) : null,
                client != null && client.getLogoPath() != null && !client.getLogoPath().isBlank(),
                client != null ? firstNonBlank(client.getEmailReceiverNom(), client.getContactNom()) : null,
                client != null ? client.getEmailReceiverGenre() : null,
                facture.getDateEmission(),
                facture.getDateEcheance(),
                totalTtc,
                paye,
                Math.max(0.0, totalTtc - paye),
                facture.getStatut(),
                joursDeRetard,
                toneFor(joursDeRetard, config));
    }

    private static Tone toneFor(long joursDeRetard, AiProperties.Reminder config) {
        if (joursDeRetard <= config.getSoftMaxDays()) {
            return Tone.SOFT;
        }
        if (joursDeRetard <= config.getFirmMaxDays()) {
            return Tone.FIRM;
        }
        return Tone.FORMAL;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }

    /** Civilite deduite du genre du destinataire, neutre par defaut. */
    public String civilite() {
        if ("M".equalsIgnoreCase(destinataireGenre)) {
            return "Monsieur";
        }
        if ("F".equalsIgnoreCase(destinataireGenre)) {
            return "Madame";
        }
        return "Madame, Monsieur";
    }

    /** Bloc de faits injecte au LLM, pour qu'il n'invente aucun chiffre. */
    public String toPromptBlock() {
        StringBuilder sb = new StringBuilder();
        sb.append("Facture : ").append(numero).append('\n');
        sb.append("Client : ").append(clientNom == null ? "-" : clientNom).append('\n');
        sb.append("Destinataire : ").append(destinataireNom == null ? "-" : destinataireNom)
                .append(" (civilite : ").append(civilite()).append(")\n");
        sb.append("Date d'emission : ").append(dateEmission).append('\n');
        sb.append("Date d'echeance : ").append(dateEcheance).append('\n');
        sb.append("Montant TTC : ").append(format(totalTtc)).append(" DT\n");
        sb.append("Deja regle : ").append(format(montantPaye)).append(" DT\n");
        sb.append("Reste du : ").append(format(resteDu)).append(" DT\n");
        sb.append("Statut : ").append(statut == null ? "-" : statut.name()).append('\n');
        sb.append("Jours de retard : ").append(joursDeRetard).append('\n');
        sb.append("Palier de ton impose : ").append(tone.name()).append('\n');
        return sb.toString();
    }

    private static String format(double amount) {
        return String.format(java.util.Locale.FRANCE, "%.3f", amount);
    }
}
