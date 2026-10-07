package com.antigone.rh.bi.dto;

import java.util.List;

/**
 * Tableau de bord « Presence et productivite ».
 * Chaque liste correspond a un visuel de la page Analytique RH.
 */
public record PresenceDashboardDTO(
        Kpis kpis,
        List<PointMensuel> evolutionMensuelle,
        List<RepartitionStatut> repartitionStatuts,
        List<LigneDepartement> parDepartement,
        List<LigneEmploye> topRetards,
        List<PointInactivite> inactivite,
        List<LigneConge> conges) {

    public record Kpis(
            long joursSuivis,
            long effectifSuivi,
            double tauxPresence,
            double tauxAbsenteisme,
            double tauxTeletravail,
            double heuresTravaillees,
            double retardMoyenMinutes,
            long joursRetard) {
    }

    public record PointMensuel(
            String mois,
            String libelle,
            double tauxPresence,
            double tauxAbsenteisme,
            double heures) {
    }

    public record RepartitionStatut(String statut, long jours) {
    }

    public record LigneDepartement(
            String departement,
            long effectif,
            double tauxPresence,
            double retardMoyenMinutes,
            double heures) {
    }

    public record LigneEmploye(
            String employe,
            String departement,
            long retardTotalMinutes,
            long joursRetard,
            double heures) {
    }

    public record PointInactivite(
            String semaine,
            long minutesExcedentaires,
            double montantDeduction) {
    }

    public record LigneConge(String typeConge, long jours, long demandes) {
    }
}
