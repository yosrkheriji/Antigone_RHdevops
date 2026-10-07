package com.antigone.rh.bi.dto;

import java.util.List;

/** Tableau de bord « Projets » : avancement, delais et charge par collaborateur. */
public record ProjetsDashboardDTO(
        Kpis kpis,
        List<RepartitionStatut> repartitionProjets,
        List<PointMensuel> evolutionMensuelle,
        List<LigneCollaborateur> chargeParCollaborateur,
        List<ProjetARisque> projetsARisque,
        List<LigneDelai> delaiParProjet) {

    public record Kpis(
            long projetsTotal,
            long projetsEnCours,
            long projetsClotures,
            long tachesTotal,
            long tachesTerminees,
            long tachesEnRetard,
            double tauxCompletion,
            double delaiMoyenTacheJours,
            double dureeMoyenneProjetJours) {
    }

    public record RepartitionStatut(String statut, long nombre) {
    }

    public record PointMensuel(
            String mois,
            String libelle,
            long tachesCreees,
            long tachesTerminees,
            long tachesEnRetard) {
    }

    public record LigneCollaborateur(
            String employe,
            String departement,
            long tachesTotal,
            long tachesTerminees,
            long tachesEnRetard,
            double tauxCompletion,
            double delaiMoyenJours) {
    }

    public record ProjetARisque(
            String projet,
            String client,
            String statut,
            String dateFin,
            long tachesTotal,
            long tachesEnRetard,
            double tauxCompletion) {
    }

    public record LigneDelai(
            String projet,
            double delaiMoyenJours,
            double ecartMoyenJours,
            long tachesTerminees) {
    }
}
